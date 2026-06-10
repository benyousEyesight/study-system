package com.study.paper.exam.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.study.paper.exam.mapper.ExamAnswerMapper;
import com.study.paper.exam.mapper.ExamMapper;
import com.study.paper.exam.mapper.ExamSessionMapper;
import com.study.paper.exam.mapper.PaperQualityMapper;
import com.study.paper.exam.model.dto.PaperQualityVO;
import com.study.paper.exam.model.entity.Exam;
import com.study.paper.exam.model.entity.ExamAnswer;
import com.study.paper.exam.model.entity.ExamSession;
import com.study.paper.exam.model.entity.PaperQuality;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class PaperQualityService {

    @Autowired
    private ExamMapper examMapper;
    @Autowired
    private ExamSessionMapper sessionMapper;
    @Autowired
    private ExamAnswerMapper answerMapper;
    @Autowired
    private PaperQualityMapper qualityMapper;
    @Autowired
    private ObjectMapper objectMapper;

    public PaperQualityVO getQuality(Long examId) throws Exception {
        PaperQuality cached = qualityMapper.selectOne(
                new LambdaQueryWrapper<PaperQuality>().eq(PaperQuality::getExamId, examId));
        if (cached != null) {
            return toVO(cached);
        }
        return computeQuality(examId);
    }

    public PaperQualityVO computeQuality(Long examId) throws Exception {
        Exam exam = examMapper.selectById(examId);
        if (exam == null) return null;

        List<ExamSession> graded = sessionMapper.selectList(
                new LambdaQueryWrapper<ExamSession>()
                        .eq(ExamSession::getExamId, examId)
                        .eq(ExamSession::getStatus, "GRADED")
                        .orderByAsc(ExamSession::getTotalScore));

        if (graded.isEmpty()) return null;

        // Collect all questionIds and parse snapshot for max scores
        List<Long> sessionIds = graded.stream().map(ExamSession::getId).collect(Collectors.toList());

        // Get max scores from first session's paper_snapshot
        Map<Long, BigDecimal> maxScoreMap = new LinkedHashMap<>();
        List<Long> questionOrder = new ArrayList<>();
        try {
            Map<String, Object> snapshot = objectMapper.readValue(
                    graded.get(0).getPaperSnapshot(), new TypeReference<Map<String, Object>>() {});
            List<Map<String, Object>> sections = (List<Map<String, Object>>) snapshot.get("sections");
            if (sections != null) {
                for (Map<String, Object> section : sections) {
                    List<Map<String, Object>> questions = (List<Map<String, Object>>) section.get("questions");
                    if (questions != null) {
                        for (Map<String, Object> q : questions) {
                            Long qId = ((Number) q.get("questionId")).longValue();
                            BigDecimal score = new BigDecimal(q.get("score").toString());
                            maxScoreMap.put(qId, score);
                            questionOrder.add(qId);
                        }
                    }
                }
            }
        } catch (Exception e) {
            return null;
        }

        // Get all answers for these sessions
        List<ExamAnswer> allAnswers = answerMapper.selectList(
                new LambdaQueryWrapper<ExamAnswer>()
                        .in(ExamAnswer::getSessionId, sessionIds));

        if (allAnswers.isEmpty()) return null;

        // Group answers by questionId
        Map<Long, List<ExamAnswer>> byQuestion = allAnswers.stream()
                .collect(Collectors.groupingBy(ExamAnswer::getQuestionId));

        // Calculate per-question stats
        List<PaperQualityVO.QuestionQuality> questions = new ArrayList<>();
        int sortIdx = 0;
        double sumVariance = 0;
        List<Double> totalScores = new ArrayList<>();

        for (Long qId : questionOrder) {
            List<ExamAnswer> qAnswers = byQuestion.get(qId);
            if (qAnswers == null || qAnswers.isEmpty()) continue;

            BigDecimal maxScore = maxScoreMap.getOrDefault(qId, BigDecimal.ONE);
            BigDecimal sumScore = BigDecimal.ZERO;
            int count = 0;

            // Build sessionId -> score map
            Map<Long, BigDecimal> sessionScoreMap = new HashMap<>();
            for (ExamAnswer a : qAnswers) {
                if (a.getScore() != null) {
                    sumScore = sumScore.add(a.getScore());
                    count++;
                    sessionScoreMap.merge(a.getSessionId(), a.getScore(), BigDecimal::add);
                }
            }

            // Also build per-session total scores
            if (sortIdx == 0) {
                // first question, initialize totalScores
                for (ExamSession s : graded) {
                    totalScores.add(s.getTotalScore() != null ? s.getTotalScore().doubleValue() : 0);
                }
            }

            BigDecimal avgScore = count > 0 ? sumScore.divide(BigDecimal.valueOf(count), 2, RoundingMode.HALF_UP) : BigDecimal.ZERO;
            BigDecimal difficulty = maxScore.compareTo(BigDecimal.ZERO) > 0
                    ? avgScore.divide(maxScore, 4, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;

            // Variance per question
            double mean = avgScore.doubleValue();
            double variance = 0;
            for (ExamAnswer a : qAnswers) {
                double sc = a.getScore() != null ? a.getScore().doubleValue() : 0;
                variance += Math.pow(sc - mean, 2);
            }
            variance /= qAnswers.size();
            sumVariance += variance;

            // Discrimination: compare top 27% vs bottom 27%
            int groupSize = Math.max(1, graded.size() * 27 / 100);
            // Already sorted by totalScore ascending
            List<ExamSession> lowGroup = graded.subList(0, Math.min(groupSize, graded.size()));
            List<ExamSession> highGroup = graded.subList(Math.max(0, graded.size() - groupSize), graded.size());

            double lowAvg = 0, highAvg = 0;
            int lowCorrect = 0, highCorrect = 0;

            for (ExamSession s : lowGroup) {
                ExamAnswer match = qAnswers.stream()
                        .filter(a -> a.getSessionId().equals(s.getId()))
                        .findFirst().orElse(null);
                double sc = match != null && match.getScore() != null ? match.getScore().doubleValue() : 0;
                lowAvg += sc;
                if (match != null && match.getIsCorrect() != null && match.getIsCorrect() == 1) lowCorrect++;
            }
            for (ExamSession s : highGroup) {
                ExamAnswer match = qAnswers.stream()
                        .filter(a -> a.getSessionId().equals(s.getId()))
                        .findFirst().orElse(null);
                double sc = match != null && match.getScore() != null ? match.getScore().doubleValue() : 0;
                highAvg += sc;
                if (match != null && match.getIsCorrect() != null && match.getIsCorrect() == 1) highCorrect++;
            }

            lowAvg /= lowGroup.size();
            highAvg /= highGroup.size();
            BigDecimal discrimination = maxScore.compareTo(BigDecimal.ZERO) > 0
                    ? BigDecimal.valueOf((highAvg - lowAvg) / maxScore.doubleValue()).setScale(4, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;

            PaperQualityVO.QuestionQuality qq = new PaperQualityVO.QuestionQuality();
            qq.setSort(++sortIdx);
            qq.setQuestionId(qId);
            qq.setMaxScore(maxScore);
            qq.setAvgScore(avgScore);
            qq.setDifficulty(difficulty);
            qq.setDiscrimination(discrimination);
            qq.setHighGroupCorrect(highCorrect);
            qq.setLowGroupCorrect(lowCorrect);
            questions.add(qq);
        }

        // Calculate Cronbach's α (reliability)
        double totalVariance = 0;
        if (!totalScores.isEmpty()) {
            double totalMean = totalScores.stream().mapToDouble(Double::doubleValue).average().orElse(0);
            totalVariance = totalScores.stream().mapToDouble(s -> Math.pow(s - totalMean, 2)).sum() / totalScores.size();
        }

        int k = questions.size();
        double alpha = 0;
        if (k > 1 && totalVariance > 0) {
            alpha = ((double) k / (k - 1)) * (1 - (sumVariance / totalVariance));
        }
        alpha = Math.min(1, Math.max(0, alpha));

        // Overall difficulty (weighted by max score)
        BigDecimal totalMax = BigDecimal.ZERO;
        BigDecimal totalAvg = BigDecimal.ZERO;
        for (PaperQualityVO.QuestionQuality qq : questions) {
            totalMax = totalMax.add(qq.getMaxScore());
        }
        double overallDifficulty = totalMax.doubleValue() > 0
                ? graded.stream().mapToDouble(s -> s.getTotalScore() != null ? s.getTotalScore().doubleValue() : 0).average().orElse(0) / totalMax.doubleValue()
                : 0;

        // Overall discrimination (average of per-question)
        double overallDiscrimination = questions.stream()
                .mapToDouble(q -> q.getDiscrimination().doubleValue())
                .average().orElse(0);

        // Save to DB
        PaperQuality quality = new PaperQuality();
        quality.setExamId(examId);
        quality.setPaperId(exam.getPaperId());
        quality.setDifficultyIndex(BigDecimal.valueOf(overallDifficulty).setScale(4, RoundingMode.HALF_UP));
        quality.setDiscriminationIndex(BigDecimal.valueOf(overallDiscrimination).setScale(4, RoundingMode.HALF_UP));
        quality.setReliabilityIndex(BigDecimal.valueOf(alpha).setScale(4, RoundingMode.HALF_UP));
        quality.setTotalQuestions(k);
        quality.setTotalStudents(graded.size());
        quality.setDetails(objectMapper.writeValueAsString(questions));
        qualityMapper.insert(quality);

        return toVO(quality);
    }

    private PaperQualityVO toVO(PaperQuality pq) throws Exception {
        PaperQualityVO vo = new PaperQualityVO();
        vo.setExamId(pq.getExamId());
        vo.setPaperId(pq.getPaperId());
        vo.setTotalQuestions(pq.getTotalQuestions());
        vo.setTotalStudents(pq.getTotalStudents());
        vo.setDifficultyIndex(pq.getDifficultyIndex());
        vo.setDiscriminationIndex(pq.getDiscriminationIndex());
        vo.setReliabilityIndex(pq.getReliabilityIndex());

        vo.setDifficultyLabel(labelDifficulty(pq.getDifficultyIndex()));
        vo.setDiscriminationLabel(labelDiscrimination(pq.getDiscriminationIndex()));
        vo.setReliabilityLabel(labelReliability(pq.getReliabilityIndex()));

        if (pq.getDetails() != null) {
            List<PaperQualityVO.QuestionQuality> qs = objectMapper.readValue(
                    pq.getDetails(), new TypeReference<List<PaperQualityVO.QuestionQuality>>() {});
            vo.setQuestions(qs);
        } else {
            vo.setQuestions(Collections.emptyList());
        }
        return vo;
    }

    private String labelDifficulty(BigDecimal d) {
        if (d == null) return "未知";
        double v = d.doubleValue();
        if (v < 0.3) return "偏难";
        if (v <= 0.7) return "适中";
        return "偏易";
    }

    private String labelDiscrimination(BigDecimal d) {
        if (d == null) return "未知";
        double v = d.doubleValue();
        if (v >= 0.4) return "优秀";
        if (v >= 0.3) return "良好";
        if (v >= 0.2) return "合格";
        return "较差";
    }

    private String labelReliability(BigDecimal d) {
        if (d == null) return "未知";
        double v = d.doubleValue();
        if (v >= 0.9) return "很高";
        if (v >= 0.8) return "良好";
        if (v >= 0.7) return "可接受";
        return "较低";
    }
}
