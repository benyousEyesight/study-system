package com.study.paper.exam.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.study.paper.exam.mapper.ExamAnswerMapper;
import com.study.paper.exam.mapper.ExamMapper;
import com.study.paper.exam.mapper.ExamSessionMapper;
import com.study.paper.exam.model.dto.StudentStatsVO;
import com.study.paper.exam.model.entity.Exam;
import com.study.paper.exam.model.entity.ExamAnswer;
import com.study.paper.exam.model.entity.ExamSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class StudentStatsService {

    @Autowired
    private ExamSessionMapper sessionMapper;
    @Autowired
    private ExamMapper examMapper;
    @Autowired
    private ExamAnswerMapper answerMapper;

    public StudentStatsVO.Overview getOverview(Long userId) {
        List<ExamSession> graded = sessionMapper.selectList(
                new LambdaQueryWrapper<ExamSession>()
                        .eq(ExamSession::getUserId, userId)
                        .eq(ExamSession::getStatus, "GRADED"));

        StudentStatsVO.Overview overview = new StudentStatsVO.Overview();
        overview.setTotalGraded(graded.size());

        long totalExams = sessionMapper.selectCount(
                new LambdaQueryWrapper<ExamSession>()
                        .eq(ExamSession::getUserId, userId)
                        .in(ExamSession::getStatus, "SUBMITTED", "GRADING", "GRADED"));
        overview.setTotalExams((int) totalExams);

        if (graded.isEmpty()) {
            overview.setAvgScore(BigDecimal.ZERO);
            overview.setBestScore(BigDecimal.ZERO);
            overview.setPassRate(BigDecimal.ZERO);
            return overview;
        }

        // avg and best
        BigDecimal sum = BigDecimal.ZERO;
        BigDecimal best = BigDecimal.ZERO;
        int passed = 0;
        for (ExamSession s : graded) {
            BigDecimal score = s.getTotalScore() != null ? s.getTotalScore() : BigDecimal.ZERO;
            sum = sum.add(score);
            if (score.compareTo(best) > 0) best = score;
            // pass = score >= 60 (assuming percentage-based, or use exam total)
            if (score.compareTo(new BigDecimal("60")) >= 0) passed++;
        }
        overview.setBestScore(best);
        overview.setAvgScore(sum.divide(BigDecimal.valueOf(graded.size()), 1, RoundingMode.HALF_UP));
        overview.setPassRate(BigDecimal.valueOf(passed)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(graded.size()), 1, RoundingMode.HALF_UP));
        return overview;
    }

    public List<StudentStatsVO.SubjectStat> getSubjectStats(Long userId) {
        List<ExamSession> graded = sessionMapper.selectList(
                new LambdaQueryWrapper<ExamSession>()
                        .eq(ExamSession::getUserId, userId)
                        .eq(ExamSession::getStatus, "GRADED"));
        if (graded.isEmpty()) return Collections.emptyList();

        List<Long> sessionIds = graded.stream().map(ExamSession::getId).collect(Collectors.toList());

        List<ExamAnswer> answers = answerMapper.selectList(
                new LambdaQueryWrapper<ExamAnswer>()
                        .in(ExamAnswer::getSessionId, sessionIds));

        // get subject info for all questions
        List<Long> questionIds = answers.stream()
                .map(ExamAnswer::getQuestionId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        if (questionIds.isEmpty()) return Collections.emptyList();

        List<Map<String, Object>> subjectData = answerMapper.selectQuestionSubjects(questionIds);
        Map<Long, String> subjectNameMap = new HashMap<>();
        Map<Long, Long> questionSubjectMap = new HashMap<>();
        for (Map<String, Object> row : subjectData) {
            Long qId = ((Number) row.get("id")).longValue();
            Long subjId = row.get("subjectId") != null ? ((Number) row.get("subjectId")).longValue() : 0L;
            String name = row.get("subjectName") != null ? (String) row.get("subjectName") : "未分类";
            questionSubjectMap.put(qId, subjId);
            subjectNameMap.putIfAbsent(subjId, name);
        }

        // group by subject
        Map<Long, List<ExamAnswer>> bySubject = new HashMap<>();
        for (ExamAnswer a : answers) {
            Long subjId = questionSubjectMap.getOrDefault(a.getQuestionId(), 0L);
            bySubject.computeIfAbsent(subjId, k -> new ArrayList<>()).add(a);
        }

        List<StudentStatsVO.SubjectStat> result = new ArrayList<>();
        for (Map.Entry<Long, List<ExamAnswer>> entry : bySubject.entrySet()) {
            StudentStatsVO.SubjectStat stat = new StudentStatsVO.SubjectStat();
            stat.setSubjectId(entry.getKey());
            stat.setSubjectName(subjectNameMap.getOrDefault(entry.getKey(), "未分类"));
            List<ExamAnswer> subjectAnswers = entry.getValue();
            stat.setTotalQuestions(subjectAnswers.size());
            int correct = (int) subjectAnswers.stream()
                    .filter(a -> a.getIsCorrect() != null && a.getIsCorrect() == 1)
                    .count();
            stat.setCorrectCount(correct);
            stat.setAccuracy(BigDecimal.valueOf(correct)
                    .multiply(BigDecimal.valueOf(100))
                    .divide(BigDecimal.valueOf(subjectAnswers.size()), 1, RoundingMode.HALF_UP));
            result.add(stat);
        }

        result.sort((a, b) -> b.getAccuracy().compareTo(a.getAccuracy()));
        return result;
    }

    public List<StudentStatsVO.RecentExam> getRecentExams(Long userId, int limit) {
        List<ExamSession> graded = sessionMapper.selectList(
                new LambdaQueryWrapper<ExamSession>()
                        .eq(ExamSession::getUserId, userId)
                        .eq(ExamSession::getStatus, "GRADED")
                        .orderByDesc(ExamSession::getSubmittedAt));

        if (graded.isEmpty()) return Collections.emptyList();

        // get exam titles
        List<Long> examIds = graded.stream().map(ExamSession::getExamId).distinct().collect(Collectors.toList());
        List<Exam> exams = examMapper.selectBatchIds(examIds);
        Map<Long, String> examTitleMap = exams.stream()
                .collect(Collectors.toMap(Exam::getId, Exam::getTitle));

        List<StudentStatsVO.RecentExam> result = new ArrayList<>();
        for (ExamSession s : graded) {
            // calculate rank within this exam
            long rankCount = sessionMapper.selectCount(
                    new LambdaQueryWrapper<ExamSession>()
                            .eq(ExamSession::getExamId, s.getExamId())
                            .eq(ExamSession::getStatus, "GRADED")
                            .apply("total_score > {0}", s.getTotalScore()));
            int rank = (int) rankCount + 1;

            long totalStudents = sessionMapper.selectCount(
                    new LambdaQueryWrapper<ExamSession>()
                            .eq(ExamSession::getExamId, s.getExamId())
                            .eq(ExamSession::getStatus, "GRADED"));

            StudentStatsVO.RecentExam re = new StudentStatsVO.RecentExam();
            re.setExamId(s.getExamId());
            re.setExamTitle(examTitleMap.getOrDefault(s.getExamId(), "未知考试"));
            re.setTotalScore(s.getTotalScore());
            re.setRank(rank);
            re.setTotalStudents((int) totalStudents);
            re.setSubmittedAt(s.getSubmittedAt());
            result.add(re);

            if (result.size() >= limit) break;
        }

        return result;
    }
}
