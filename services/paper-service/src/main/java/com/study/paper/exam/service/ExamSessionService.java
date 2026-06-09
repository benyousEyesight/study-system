package com.study.paper.exam.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.study.paper.exam.mapper.ExamAnswerMapper;
import com.study.paper.exam.mapper.ExamAssignmentMapper;
import com.study.paper.exam.mapper.ExamMapper;
import com.study.paper.exam.mapper.ExamSessionMapper;
import com.study.paper.exam.model.dto.*;
import com.study.paper.exam.model.entity.Exam;
import com.study.paper.exam.model.entity.ExamAnswer;
import com.study.paper.exam.model.entity.ExamAssignment;
import com.study.paper.exam.model.entity.ExamSession;
import com.study.paper.mapper.PaperMapper;
import com.study.paper.mapper.PaperSectionMapper;
import com.study.paper.mapper.PaperQuestionMapper;
import com.study.paper.model.entity.Paper;
import com.study.paper.model.entity.PaperSection;
import com.study.paper.model.entity.PaperQuestion;
import com.study.paper.common.BusinessException;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ExamSessionService {

    @Autowired
    private ExamMapper examMapper;
    @Autowired
    private ExamAssignmentMapper assignmentMapper;
    @Autowired
    private ExamSessionMapper sessionMapper;
    @Autowired
    private ExamAnswerMapper answerMapper;
    @Autowired
    private PaperMapper paperMapper;
    @Autowired
    private PaperSectionMapper sectionMapper;
    @Autowired
    private PaperQuestionMapper paperQuestionMapper;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 获取学生可见的考试列表
     */
    public List<ExamVO> getMyExams(Long userId, Long tenantId, String userType) {
        // 查询直接分配 + 角色匹配 + 考试码公开的考试
        List<Long> assignedExamIds = assignmentMapper.selectList(
                new LambdaQueryWrapper<ExamAssignment>()
                        .eq(ExamAssignment::getAssignType, "USER")
                        .eq(ExamAssignment::getAssigneeId, userId))
                .stream().map(ExamAssignment::getExamId).collect(Collectors.toList());

        List<Exam> allExams = examMapper.selectList(
                new LambdaQueryWrapper<Exam>()
                        .eq(Exam::getTenantId, tenantId)
                        .in(Exam::getStatus, "PUBLISHED", "IN_PROGRESS")
                        .orderByDesc(Exam::getCreatedAt));

        return allExams.stream()
                .filter(e -> isStudentAssigned(e, userId, assignedExamIds))
                .map(e -> {
                    ExamVO vo = new ExamVO();
                    BeanUtils.copyProperties(e, vo);
                    // 检查学生是否已有会话
                    ExamSession session = sessionMapper.selectOne(
                            new LambdaQueryWrapper<ExamSession>()
                                    .eq(ExamSession::getExamId, e.getId())
                                    .eq(ExamSession::getUserId, userId));
                    if (session != null) {
                        vo.setTotalSessions(1);
                        vo.setGradedSessions("GRADED".equals(session.getStatus()) ? 1 : 0);
                    }
                    return vo;
                }).collect(Collectors.toList());
    }

    private boolean isStudentAssigned(Exam exam, Long userId, List<Long> assignedExamIds) {
        if (assignedExamIds.contains(exam.getId())) return true;
        // EXAM_CODE 模式的考试所有人可见
        if (exam.getExamCode() != null && !exam.getExamCode().isEmpty()) return true;
        return false;
    }

    /**
     * 通过考试码加入考试
     */
    public ExamVO joinByCode(Long examId, String code, Long userId) {
        Exam exam = examMapper.selectOne(
                new LambdaQueryWrapper<Exam>()
                        .eq(Exam::getId, examId)
                        .eq(Exam::getExamCode, code));
        if (exam == null) throw new BusinessException("考试码无效");

        // 检查是否已有会话
        ExamSession existing = sessionMapper.selectOne(
                new LambdaQueryWrapper<ExamSession>()
                        .eq(ExamSession::getExamId, examId)
                        .eq(ExamSession::getUserId, userId));
        if (existing != null) throw new BusinessException("已经参加过该考试");

        ExamVO vo = new ExamVO();
        BeanUtils.copyProperties(exam, vo);
        return vo;
    }

    /**
     * 开始考试：创建会话 + 试卷快照 + 预创建答题记录
     */
    @Transactional
    public ExamSessionVO startExam(Long examId, Long userId) {
        Exam exam = examMapper.selectById(examId);
        if (exam == null) throw new BusinessException("考试不存在");
        if (!"PUBLISHED".equals(exam.getStatus()) && !"IN_PROGRESS".equals(exam.getStatus())) {
            throw new BusinessException("考试未发布");
        }

        // 时间校验
        if ("FIXED_WINDOW".equals(exam.getTimeMode()) || "BOTH".equals(exam.getTimeMode())) {
            LocalDateTime now = LocalDateTime.now();
            if (exam.getStartTime() != null && now.isBefore(exam.getStartTime())) {
                throw new BusinessException("考试还未开始");
            }
            if (exam.getEndTime() != null && now.isAfter(exam.getEndTime())) {
                throw new BusinessException("考试已结束");
            }
        }

        // 检查是否已有进行中的会话
        ExamSession existing = sessionMapper.selectOne(
                new LambdaQueryWrapper<ExamSession>()
                        .eq(ExamSession::getExamId, examId)
                        .eq(ExamSession::getUserId, userId)
                        .in(ExamSession::getStatus, "IN_PROGRESS", "SUBMITTED", "GRADING"));
        if (existing != null) {
            if ("IN_PROGRESS".equals(existing.getStatus())) {
                // 返回已有会话
                return toSessionVO(existing, exam);
            }
            throw new BusinessException("已经参加过该考试");
        }

        // 加载试卷 + 题目快照
        Paper paper = paperMapper.selectById(exam.getPaperId());
        if (paper == null) throw new BusinessException("关联试卷不存在");

        List<PaperSection> sections = sectionMapper.selectList(
                new LambdaQueryWrapper<PaperSection>().eq(PaperSection::getPaperId, paper.getId())
                        .orderByAsc(PaperSection::getSort));

        // 收集所有 questionId
        List<Long> questionIds = new ArrayList<>();
        for (PaperSection section : sections) {
            List<PaperQuestion> pqs = paperQuestionMapper.selectList(
                    new LambdaQueryWrapper<PaperQuestion>().eq(PaperQuestion::getSectionId, section.getId())
                            .orderByAsc(PaperQuestion::getSort));
            for (PaperQuestion pq : pqs) {
                questionIds.add(pq.getQuestionId());
            }
        }

        // 跨库查询题目内容
        List<Map<String, Object>> questions = questionIds.isEmpty()
                ? Collections.emptyList()
                : answerMapper.selectQuestionsByIds(questionIds);
        Map<Long, Map<String, Object>> questionMap = questions.stream()
                .collect(Collectors.toMap(q -> (Long) q.get("id"), q -> q));

        // 构建快照
        Map<String, Object> snapshot = new HashMap<>();
        snapshot.put("paper", paper);
        List<Map<String, Object>> snapshotSections = new ArrayList<>();
        for (PaperSection section : sections) {
            Map<String, Object> secMap = new HashMap<>();
            secMap.put("id", section.getId());
            secMap.put("title", section.getTitle());
            secMap.put("sort", section.getSort());
            secMap.put("totalScore", section.getTotalScore());

            List<PaperQuestion> pqs = paperQuestionMapper.selectList(
                    new LambdaQueryWrapper<PaperQuestion>().eq(PaperQuestion::getSectionId, section.getId())
                            .orderByAsc(PaperQuestion::getSort));
            List<Map<String, Object>> snapshotQuestions = new ArrayList<>();
            for (PaperQuestion pq : pqs) {
                Map<String, Object> qMap = new HashMap<>();
                qMap.put("questionId", pq.getQuestionId());
                qMap.put("sort", pq.getSort());
                qMap.put("score", pq.getScore());
                Map<String, Object> qData = questionMap.get(pq.getQuestionId());
                if (qData != null) {
                    qMap.put("type", qData.get("type"));
                    qMap.put("contentJson", qData.get("contentJson"));
                    qMap.put("answerJson", qData.get("answerJson"));
                }
                snapshotQuestions.add(qMap);
            }
            secMap.put("questions", snapshotQuestions);
            snapshotSections.add(secMap);
        }
        snapshot.put("sections", snapshotSections);

        // 创建会话
        ExamSession session = new ExamSession();
        session.setExamId(examId);
        session.setUserId(userId);
        try {
            session.setPaperSnapshot(objectMapper.writeValueAsString(snapshot));
        } catch (Exception e) {
            throw new BusinessException("试卷快照序列化失败");
        }
        session.setStartTime(LocalDateTime.now());
        session.setStatus("IN_PROGRESS");
        session.setTabSwitchCount(0);
        sessionMapper.insert(session);

        // 预创建答题记录
        for (PaperSection section : sections) {
            List<PaperQuestion> pqs = paperQuestionMapper.selectList(
                    new LambdaQueryWrapper<PaperQuestion>().eq(PaperQuestion::getSectionId, section.getId()));
            for (PaperQuestion pq : pqs) {
                ExamAnswer answer = new ExamAnswer();
                answer.setSessionId(session.getId());
                answer.setSectionId(section.getId());
                answer.setQuestionId(pq.getQuestionId());
                Map<String, Object> qData = questionMap.get(pq.getQuestionId());
                answer.setQuestionType(qData != null ? (String) qData.get("type") : null);
                answer.setGradingStatus("UNGRADED");
                answerMapper.insert(answer);
            }
        }

        return toSessionVO(session, exam);
    }

    /**
     * 获取考试页面数据
     */
    public ExamSessionVO getSessionData(Long sessionId, Long userId) {
        ExamSession session = sessionMapper.selectById(sessionId);
        if (session == null || !session.getUserId().equals(userId)) {
            throw new BusinessException("会话不存在");
        }
        Exam exam = examMapper.selectById(session.getExamId());
        return toSessionVO(session, exam);
    }

    /**
     * 保存单题答案
     */
    @Transactional
    public void saveAnswer(Long sessionId, Long userId, AnswerSubmitDTO dto) {
        ExamSession session = sessionMapper.selectById(sessionId);
        if (session == null || !session.getUserId().equals(userId)) {
            throw new BusinessException("会话不存在");
        }
        if (!"IN_PROGRESS".equals(session.getStatus())) {
            throw new BusinessException("考试已结束");
        }

        ExamAnswer answer = answerMapper.selectOne(
                new LambdaQueryWrapper<ExamAnswer>()
                        .eq(ExamAnswer::getSessionId, sessionId)
                        .eq(ExamAnswer::getQuestionId, dto.getQuestionId()));
        if (answer != null) {
            answer.setAnswerJson(dto.getAnswerJson());
            answer.setAnsweredAt(LocalDateTime.now());
            answerMapper.updateById(answer);
        }
    }

    /**
     * 提交全卷 + 自动批改客观题
     */
    @Transactional
    public void submitExam(Long sessionId, Long userId) {
        ExamSession session = sessionMapper.selectById(sessionId);
        if (session == null || !session.getUserId().equals(userId)) {
            throw new BusinessException("会话不存在");
        }
        if (!"IN_PROGRESS".equals(session.getStatus())) {
            throw new BusinessException("考试已提交");
        }

        // 解析快照获取题目答案
        List<Map<String, Object>> questions = extractQuestionsFromSnapshot(session.getPaperSnapshot());
        Map<Long, String> correctAnswers = new HashMap<>();
        Map<Long, String> questionTypes = new HashMap<>();
        for (Map<String, Object> q : questions) {
            Long qId = ((Number) q.get("questionId")).longValue();
            String answerJson = (String) q.get("answerJson");
            String type = (String) q.get("type");
            if (answerJson != null) {
                correctAnswers.put(qId, answerJson);
            }
            if (type != null) {
                questionTypes.put(qId, type);
            }
        }

        // 自动批改客观题
        List<ExamAnswer> answers = answerMapper.selectList(
                new LambdaQueryWrapper<ExamAnswer>().eq(ExamAnswer::getSessionId, sessionId));
        for (ExamAnswer answer : answers) {
            String type = answer.getQuestionType() != null ? answer.getQuestionType() : questionTypes.get(answer.getQuestionId());
            if (type == null) continue;

            if (isAutoGradable(type) && answer.getAnswerJson() != null && correctAnswers.containsKey(answer.getQuestionId())) {
                boolean correct = gradeAnswer(type, answer.getAnswerJson(), correctAnswers.get(answer.getQuestionId()));
                answer.setIsCorrect(correct ? 1 : 0);
                answer.setScore(correct ? getQuestionScore(questions, answer.getQuestionId()) : BigDecimal.ZERO);
                answer.setGradingStatus("AUTO_GRADED");
                answer.setGradedAt(LocalDateTime.now());
                answerMapper.updateById(answer);
            }
        }

        // 更新会话状态
        session.setStatus("SUBMITTED");
        session.setSubmittedAt(LocalDateTime.now());
        session.setDurationMinutes((int) ChronoUnit.MINUTES.between(session.getStartTime(), LocalDateTime.now()));
        sessionMapper.updateById(session);
    }

    /**
     * 心跳：同步切屏次数 + 检测阈值
     */
    @Transactional
    public void heartbeat(Long sessionId, Long userId, int tabSwitchCount) {
        ExamSession session = sessionMapper.selectById(sessionId);
        if (session == null || !session.getUserId().equals(userId)) return;
        if (!"IN_PROGRESS".equals(session.getStatus())) return;

        session.setTabSwitchCount(tabSwitchCount);
        sessionMapper.updateById(session);

        Exam exam = examMapper.selectById(session.getExamId());
        if (exam.getMaxTabSwitches() != null && exam.getMaxTabSwitches() > 0
                && tabSwitchCount >= exam.getMaxTabSwitches()) {
            // 自动提交
            submitExam(sessionId, userId);
            // 标记为自动提交
            session.setStatus("AUTO_SUBMITTED");
            sessionMapper.updateById(session);
        }
    }

    /**
     * 查看成绩
     */
    public ExamSessionVO getResult(Long sessionId, Long userId) {
        ExamSession session = sessionMapper.selectById(sessionId);
        if (session == null || !session.getUserId().equals(userId)) {
            throw new BusinessException("会话不存在");
        }
        if (!"GRADED".equals(session.getStatus())) {
            throw new BusinessException("成绩尚未发布");
        }

        Exam exam = examMapper.selectById(session.getExamId());
        ExamSessionVO vo = toSessionVO(session, exam);

        List<ExamAnswer> answers = answerMapper.selectList(
                new LambdaQueryWrapper<ExamAnswer>().eq(ExamAnswer::getSessionId, sessionId));
        List<ExamSessionVO.AnswerResultVO> resultList = new ArrayList<>();

        List<Map<String, Object>> snapshotQuestions = extractQuestionsFromSnapshot(session.getPaperSnapshot());
        Map<Long, Map<String, Object>> questionMap = snapshotQuestions.stream()
                .collect(Collectors.toMap(q -> ((Number) q.get("questionId")).longValue(), q -> q));

        for (ExamAnswer answer : answers) {
            ExamSessionVO.AnswerResultVO r = new ExamSessionVO.AnswerResultVO();
            r.setQuestionId(answer.getQuestionId());
            r.setQuestionType(answer.getQuestionType());
            r.setStudentAnswer(answer.getAnswerJson());
            r.setScore(answer.getScore());
            r.setGradingStatus(answer.getGradingStatus());
            r.setGraderComment(answer.getGraderComment());

            Map<String, Object> qData = questionMap.get(answer.getQuestionId());
            if (qData != null) {
                r.setCorrectAnswer((String) qData.get("answerJson"));
                r.setTotalScore(new BigDecimal(qData.get("score").toString()));
                String contentJson = (String) qData.get("contentJson");
                if (contentJson != null) {
                    try {
                        Map<String, Object> content = objectMapper.readValue(contentJson, new TypeReference<Map<String, Object>>() {});
                        r.setQuestionContent((String) content.getOrDefault("text", contentJson));
                    } catch (Exception e) {
                        r.setQuestionContent(contentJson);
                    }
                }
            }
            r.setIsCorrect(answer.getIsCorrect());
            resultList.add(r);
        }
        vo.setAnswers(resultList);
        return vo;
    }

    // --- 辅助方法 ---

    private ExamSessionVO toSessionVO(ExamSession session, Exam exam) {
        ExamSessionVO vo = new ExamSessionVO();
        BeanUtils.copyProperties(session, vo);
        vo.setUserId(session.getUserId());

        ExamSessionVO.ExamBasicVO basic = new ExamSessionVO.ExamBasicVO();
        basic.setTitle(exam.getTitle());
        basic.setDurationMinutes(exam.getDurationMinutes());
        basic.setMaxTabSwitches(exam.getMaxTabSwitches());

        if ("IN_PROGRESS".equals(session.getStatus()) && exam.getDurationMinutes() != null) {
            long elapsed = ChronoUnit.SECONDS.between(session.getStartTime(), LocalDateTime.now());
            int remaining = (int) (exam.getDurationMinutes() * 60 - elapsed);
            basic.setRemainingSeconds(Math.max(0, remaining));
        } else {
            basic.setRemainingSeconds(0);
        }
        vo.setExamInfo(basic);

        // 解析快照构建题目列表
        try {
            Map<String, Object> snapshot = objectMapper.readValue(session.getPaperSnapshot(), new TypeReference<Map<String, Object>>() {});
            List<Map<String, Object>> snapSections = (List<Map<String, Object>>) snapshot.get("sections");
            if (snapSections != null) {
                List<ExamSessionVO.SectionVO> sectionVOs = new ArrayList<>();
                for (Map<String, Object> sec : snapSections) {
                    ExamSessionVO.SectionVO svo = new ExamSessionVO.SectionVO();
                    svo.setId(((Number) sec.get("id")).longValue());
                    svo.setTitle((String) sec.get("title"));
                    svo.setSort((Integer) sec.get("sort"));
                    svo.setTotalScore((Integer) sec.get("totalScore"));

                    List<Map<String, Object>> qs = (List<Map<String, Object>>) sec.get("questions");
                    if (qs != null) {
                        List<ExamSessionVO.QuestionVO> qVOs = new ArrayList<>();
                        for (Map<String, Object> q : qs) {
                            ExamSessionVO.QuestionVO qvo = new ExamSessionVO.QuestionVO();
                            qvo.setQuestionId(((Number) q.get("questionId")).longValue());
                            qvo.setSort((Integer) q.get("sort"));
                            qvo.setScore((Integer) q.get("score"));
                            qvo.setType((String) q.get("type"));
                            String contentJson = (String) q.get("contentJson");
                            if (contentJson != null) {
                                try {
                                    Map<String, Object> content = objectMapper.readValue(contentJson, new TypeReference<Map<String, Object>>() {});
                                    qvo.setContent(content);
                                } catch (Exception e) {
                                    Map<String, Object> fallback = new HashMap<>();
                                    fallback.put("text", contentJson);
                                    qvo.setContent(fallback);
                                }
                            }
                            qVOs.add(qvo);
                        }
                        svo.setQuestions(qVOs);
                    }
                    sectionVOs.add(svo);
                }
                vo.setSections(sectionVOs);
            }
        } catch (Exception e) {
            throw new BusinessException("试卷快照解析失败");
        }

        return vo;
    }

    private List<Map<String, Object>> extractQuestionsFromSnapshot(String snapshotJson) {
        List<Map<String, Object>> result = new ArrayList<>();
        try {
            Map<String, Object> snapshot = objectMapper.readValue(snapshotJson, new TypeReference<Map<String, Object>>() {});
            List<Map<String, Object>> sections = (List<Map<String, Object>>) snapshot.get("sections");
            if (sections != null) {
                for (Map<String, Object> sec : sections) {
                    List<Map<String, Object>> qs = (List<Map<String, Object>>) sec.get("questions");
                    if (qs != null) result.addAll(qs);
                }
            }
        } catch (Exception ignored) {}
        return result;
    }

    private BigDecimal getQuestionScore(List<Map<String, Object>> questions, Long questionId) {
        return questions.stream()
                .filter(q -> ((Number) q.get("questionId")).longValue() == questionId)
                .findFirst()
                .map(q -> new BigDecimal(q.get("score").toString()))
                .orElse(BigDecimal.ZERO);
    }

    private boolean isAutoGradable(String type) {
        return Set.of("SINGLE_CHOICE", "MULTIPLE_CHOICE", "TRUE_FALSE", "FILL_BLANK").contains(type);
    }

    private boolean gradeAnswer(String type, String studentAnswer, String correctAnswer) {
        if (studentAnswer == null || correctAnswer == null) return false;
        try {
            switch (type) {
                case "SINGLE_CHOICE":
                case "TRUE_FALSE":
                    return studentAnswer.trim().equalsIgnoreCase(correctAnswer.trim());
                case "MULTIPLE_CHOICE":
                    Set<String> sSet = objectMapper.readValue(studentAnswer, new TypeReference<Set<String>>() {});
                    Set<String> cSet = objectMapper.readValue(correctAnswer, new TypeReference<Set<String>>() {});
                    return sSet.equals(cSet);
                case "FILL_BLANK":
                    return studentAnswer.trim().equals(correctAnswer.trim());
                default:
                    return false;
            }
        } catch (Exception e) {
            return studentAnswer.trim().equals(correctAnswer.trim());
        }
    }
}
