package com.study.paper.exam.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.study.paper.exam.mapper.ExamAnswerMapper;
import com.study.paper.exam.mapper.ExamMapper;
import com.study.paper.exam.mapper.ExamSessionMapper;
import com.study.paper.exam.model.dto.ExamSessionVO;
import com.study.paper.exam.model.dto.ExamVO;
import com.study.paper.exam.model.dto.GradeSubmitDTO;
import com.study.paper.exam.model.entity.Exam;
import com.study.paper.exam.model.entity.ExamAnswer;
import com.study.paper.exam.model.entity.ExamSession;
import com.study.paper.common.BusinessException;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class GradingService {

    @Autowired
    private ExamMapper examMapper;
    @Autowired
    private ExamSessionMapper sessionMapper;
    @Autowired
    private ExamAnswerMapper answerMapper;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 待批改考试列表（有已提交未批改完的考试）
     */
    public List<ExamVO> getGradingExams(Long tenantId) {
        List<Exam> exams = examMapper.selectList(
                new LambdaQueryWrapper<Exam>()
                        .eq(Exam::getTenantId, tenantId)
                        .orderByDesc(Exam::getCreatedAt));
        return exams.stream().map(e -> {
            ExamVO vo = new ExamVO();
            BeanUtils.copyProperties(e, vo);

            long total = sessionMapper.selectCount(new LambdaQueryWrapper<ExamSession>()
                    .eq(ExamSession::getExamId, e.getId())
                    .in(ExamSession::getStatus, "SUBMITTED", "GRADING", "GRADED"));
            long graded = sessionMapper.selectCount(new LambdaQueryWrapper<ExamSession>()
                    .eq(ExamSession::getExamId, e.getId())
                    .eq(ExamSession::getStatus, "GRADED"));

            vo.setTotalSessions((int) total);
            vo.setGradedSessions((int) graded);
            return vo;
        }).collect(Collectors.toList());
    }

    /**
     * 获取某考试下的待批改会话
     */
    public List<ExamSessionVO> getSessionsForGrading(Long examId, Long tenantId) {
        List<ExamSession> sessions = sessionMapper.selectList(
                new LambdaQueryWrapper<ExamSession>()
                        .eq(ExamSession::getExamId, examId)
                        .in(ExamSession::getStatus, "SUBMITTED", "GRADING")
                        .orderByAsc(ExamSession::getSubmittedAt));

        return sessions.stream().map(s -> {
            ExamSessionVO vo = new ExamSessionVO();
            BeanUtils.copyProperties(s, vo);
            return vo;
        }).collect(Collectors.toList());
    }

    /**
     * 获取某学生答卷详情（用于批改）
     */
    public ExamSessionVO getSessionForGrading(Long sessionId) {
        ExamSession session = sessionMapper.selectById(sessionId);
        if (session == null) throw new BusinessException("会话不存在");

        Exam exam = examMapper.selectById(session.getExamId());
        ExamSessionVO vo = new ExamSessionVO();
        BeanUtils.copyProperties(session, vo);

        // 构建题目列表（从快照）
        List<Map<String, Object>> snapshotQuestions = extractQuestionsFromSnapshot(session.getPaperSnapshot());

        // 获取学生答案
        List<ExamAnswer> answers = answerMapper.selectList(
                new LambdaQueryWrapper<ExamAnswer>().eq(ExamAnswer::getSessionId, sessionId));

        List<ExamSessionVO.AnswerResultVO> resultList = new ArrayList<>();
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
            resultList.add(r);
        }
        vo.setAnswers(resultList);
        return vo;
    }

    /**
     * 提交批改（逐题给分 + 评语）
     */
    @Transactional
    public void gradeSession(Long sessionId, Long graderId, GradeSubmitDTO dto) {
        ExamSession session = sessionMapper.selectById(sessionId);
        if (session == null) throw new BusinessException("会话不存在");

        for (GradeSubmitDTO.GradeItem item : dto.getGrades()) {
            ExamAnswer answer = answerMapper.selectOne(
                    new LambdaQueryWrapper<ExamAnswer>()
                            .eq(ExamAnswer::getSessionId, sessionId)
                            .eq(ExamAnswer::getQuestionId, item.getQuestionId()));
            if (answer != null && !"AUTO_GRADED".equals(answer.getGradingStatus())) {
                answer.setScore(item.getScore());
                answer.setGraderId(graderId);
                answer.setGraderComment(item.getComment());
                answer.setGradingStatus("MANUAL_GRADED");
                answer.setGradedAt(LocalDateTime.now());
                answerMapper.updateById(answer);
            }
        }

        // 更新会话状态为 GRADING（可能还有未批改的）
        if (!"GRADED".equals(session.getStatus())) {
            session.setStatus("GRADING");
            sessionMapper.updateById(session);
        }
    }

    /**
     * 发布成绩：检查是否全部批改完成，计算总分
     */
    @Transactional
    public void releaseGrades(Long sessionId) {
        ExamSession session = sessionMapper.selectById(sessionId);
        if (session == null) throw new BusinessException("会话不存在");

        List<ExamAnswer> answers = answerMapper.selectList(
                new LambdaQueryWrapper<ExamAnswer>().eq(ExamAnswer::getSessionId, sessionId));

        // 检查是否有未批改的主观题
        boolean hasUngraded = answers.stream()
                .anyMatch(a -> "UNGRADED".equals(a.getGradingStatus()));
        if (hasUngraded) {
            throw new BusinessException("还有题目未批改，无法发布成绩");
        }

        // 计算总分
        BigDecimal total = answers.stream()
                .filter(a -> a.getScore() != null)
                .map(ExamAnswer::getScore)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        session.setTotalScore(total);
        session.setStatus("GRADED");
        sessionMapper.updateById(session);
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
}
