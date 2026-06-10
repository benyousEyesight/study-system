package com.study.paper.exam.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.study.paper.exam.mapper.ExamAnswerMapper;
import com.study.paper.exam.mapper.ExamSessionMapper;
import com.study.paper.exam.mapper.KpWeaknessMapper;
import com.study.paper.exam.model.dto.KpWeaknessVO;
import com.study.paper.exam.model.entity.ExamAnswer;
import com.study.paper.exam.model.entity.ExamSession;
import com.study.paper.exam.model.entity.KpWeakness;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class KpWeaknessService {

    @Autowired
    private ExamSessionMapper sessionMapper;
    @Autowired
    private ExamAnswerMapper answerMapper;
    @Autowired
    private KpWeaknessMapper weaknessMapper;
    @Autowired
    private ObjectMapper objectMapper;

    /**
     * Compute weakness data for a specific exam (called after grading completes)
     */
    @Transactional
    public void computeForExam(Long examId) {
        List<ExamSession> gradedSessions = sessionMapper.selectList(
                new LambdaQueryWrapper<ExamSession>()
                        .eq(ExamSession::getExamId, examId)
                        .eq(ExamSession::getStatus, "GRADED"));

        for (ExamSession session : gradedSessions) {
            computeForSession(session);
        }
    }

    /**
     * Compute weakness data for a specific student (full refresh)
     */
    @Transactional
    public void computeForStudent(Long studentId) {
        List<ExamSession> gradedSessions = sessionMapper.selectList(
                new LambdaQueryWrapper<ExamSession>()
                        .eq(ExamSession::getUserId, studentId)
                        .eq(ExamSession::getStatus, "GRADED"));

        // Clear old data for this student
        weaknessMapper.delete(new LambdaQueryWrapper<KpWeakness>()
                .eq(KpWeakness::getStudentId, studentId));

        for (ExamSession session : gradedSessions) {
            computeForSession(session);
        }
    }

    @SuppressWarnings("unchecked")
    private void computeForSession(ExamSession session) {
        Long studentId = session.getUserId();
        Long tenantId = null; // will be set from exam if available

        // Parse paper_snapshot to get full scores per question
        Map<Long, BigDecimal> fullScoreMap = new HashMap<>();
        try {
            Map<String, Object> snapshot = objectMapper.readValue(
                    session.getPaperSnapshot(), new TypeReference<Map<String, Object>>() {});
            List<Map<String, Object>> sections = (List<Map<String, Object>>) snapshot.get("sections");
            if (sections != null) {
                for (Map<String, Object> section : sections) {
                    List<Map<String, Object>> questions = (List<Map<String, Object>>) section.get("questions");
                    if (questions != null) {
                        for (Map<String, Object> q : questions) {
                            Long qId = ((Number) q.get("questionId")).longValue();
                            BigDecimal score = new BigDecimal(q.get("score").toString());
                            fullScoreMap.put(qId, score);
                        }
                    }
                }
            }
        } catch (Exception e) {
            // If snapshot parsing fails, skip this session
            return;
        }

        // Get all answers for this session
        List<ExamAnswer> answers = answerMapper.selectList(
                new LambdaQueryWrapper<ExamAnswer>()
                        .eq(ExamAnswer::getSessionId, session.getId()));

        if (answers.isEmpty()) return;

        // Get kp mappings for all questions
        List<Long> questionIds = answers.stream()
                .map(ExamAnswer::getQuestionId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());

        List<Map<String, Object>> kpRows = answerMapper.selectQuestionKps(questionIds);
        // Map: questionId -> List of (kpId, kpName, subjectId)
        Map<Long, List<KpMapping>> questionKpMap = new HashMap<>();
        Set<Long> seenSubjects = new HashSet<>();
        for (Map<String, Object> row : kpRows) {
            Long qId = ((Number) row.get("questionId")).longValue();
            Long kpId = ((Number) row.get("kpId")).longValue();
            String kpName = (String) row.get("kpName");
            Long subjId = row.get("subjectId") != null ? ((Number) row.get("subjectId")).longValue() : 0L;
            questionKpMap.computeIfAbsent(qId, k -> new ArrayList<>())
                    .add(new KpMapping(kpId, kpName, subjId));
            if (subjId > 0) seenSubjects.add(subjId);
        }

        // Aggregate per student+KP
        // key: "studentId:kpId" -> aggregated data
        Map<String, KpAgg> aggMap = new HashMap<>();
        for (ExamAnswer answer : answers) {
            if (answer.getScore() == null) continue;

            Long qId = answer.getQuestionId();
            List<KpMapping> mappings = questionKpMap.get(qId);
            if (mappings == null || mappings.isEmpty()) continue;

            BigDecimal earned = answer.getScore();
            BigDecimal full = fullScoreMap.getOrDefault(qId, BigDecimal.ZERO);

            for (KpMapping mapping : mappings) {
                String key = studentId + ":" + mapping.kpId;
                KpAgg agg = aggMap.computeIfAbsent(key, k -> new KpAgg());
                agg.kpId = mapping.kpId;
                agg.kpName = mapping.kpName;
                agg.subjectId = mapping.subjectId;
                agg.studentId = studentId;
                agg.earnedScore = agg.earnedScore.add(earned);
                agg.totalScore = agg.totalScore.add(full);
                agg.attemptCount++;
                if (mapping.subjectId > 0) seenSubjects.add(mapping.subjectId);
            }
        }

        // Save to DB (upsert pattern: delete existing for these kps, then insert)
        // Actually, use insert on duplicate key update via raw SQL or just delete+insert
        for (KpAgg agg : aggMap.values()) {
            KpWeakness existing = weaknessMapper.selectOne(
                    new LambdaQueryWrapper<KpWeakness>()
                            .eq(KpWeakness::getStudentId, agg.studentId)
                            .eq(KpWeakness::getKnowledgePointId, agg.kpId));

            if (existing != null) {
                existing.setTotalScore(existing.getTotalScore().add(agg.totalScore));
                existing.setEarnedScore(existing.getEarnedScore().add(agg.earnedScore));
                existing.setAttemptCount(existing.getAttemptCount() + agg.attemptCount);
                existing.setComputedAt(LocalDateTime.now());
                weaknessMapper.updateById(existing);
            } else {
                KpWeakness w = new KpWeakness();
                w.setTenantId(0L); // placeholder, would come from exam
                w.setStudentId(agg.studentId);
                w.setSubjectId(agg.subjectId);
                w.setKnowledgePointId(agg.kpId);
                w.setKnowledgePointName(agg.kpName);
                w.setTotalScore(agg.totalScore);
                w.setEarnedScore(agg.earnedScore);
                w.setAttemptCount(agg.attemptCount);
                w.setComputedAt(LocalDateTime.now());
                weaknessMapper.insert(w);
            }
        }
    }

    /**
     * Get weakness data for a student, grouped by subject
     */
    public List<KpWeaknessVO> getStudentWeakness(Long studentId) {
        List<KpWeakness> all = weaknessMapper.selectList(
                new LambdaQueryWrapper<KpWeakness>()
                        .eq(KpWeakness::getStudentId, studentId));

        if (all.isEmpty()) {
            // Auto-compute if no data
            computeForStudent(studentId);
            all = weaknessMapper.selectList(
                    new LambdaQueryWrapper<KpWeakness>()
                            .eq(KpWeakness::getStudentId, studentId));
        }

        // Resolve subject names from question_db
        Set<Long> subjectIds = all.stream()
                .map(KpWeakness::getSubjectId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, String> subjectNameMap = resolveSubjectNames(new ArrayList<>(subjectIds));

        // Group by subjectId
        Map<Long, List<KpWeakness>> bySubject = all.stream()
                .filter(w -> w.getSubjectId() != null && w.getSubjectId() > 0)
                .collect(Collectors.groupingBy(KpWeakness::getSubjectId));

        List<KpWeaknessVO> result = new ArrayList<>();
        for (Map.Entry<Long, List<KpWeakness>> entry : bySubject.entrySet()) {
            List<KpWeakness> items = entry.getValue();

            KpWeaknessVO vo = new KpWeaknessVO();
            vo.setSubjectId(entry.getKey());
            vo.setSubjectName(subjectNameMap.getOrDefault(entry.getKey(), "科目" + entry.getKey()));
            vo.setTotalKps(items.size());

            List<KpWeaknessVO.KpItem> kpItems = new ArrayList<>();
            BigDecimal subjectEarned = BigDecimal.ZERO;
            BigDecimal subjectTotal = BigDecimal.ZERO;

            for (KpWeakness w : items) {
                KpWeaknessVO.KpItem item = new KpWeaknessVO.KpItem();
                item.setKnowledgePointId(w.getKnowledgePointId());
                item.setKnowledgePointName(w.getKnowledgePointName());
                item.setEarnedScore(w.getEarnedScore());
                item.setTotalScore(w.getTotalScore());
                item.setAttemptCount(w.getAttemptCount());

                BigDecimal accuracy = w.getTotalScore().compareTo(BigDecimal.ZERO) > 0
                        ? w.getEarnedScore().multiply(BigDecimal.valueOf(100))
                            .divide(w.getTotalScore(), 1, RoundingMode.HALF_UP)
                        : BigDecimal.ZERO;
                item.setAccuracy(accuracy);

                if (accuracy.compareTo(new BigDecimal("80")) >= 0) {
                    item.setLevel("STRONG");
                } else if (accuracy.compareTo(new BigDecimal("60")) >= 0) {
                    item.setLevel("MEDIUM");
                } else {
                    item.setLevel("WEAK");
                }

                kpItems.add(item);
                subjectEarned = subjectEarned.add(w.getEarnedScore());
                subjectTotal = subjectTotal.add(w.getTotalScore());
            }

            // Sort by accuracy ascending (weakest first)
            kpItems.sort(Comparator.comparing(KpWeaknessVO.KpItem::getAccuracy));

            vo.setItems(kpItems);

            BigDecimal subjectAccuracy = subjectTotal.compareTo(BigDecimal.ZERO) > 0
                    ? subjectEarned.multiply(BigDecimal.valueOf(100))
                        .divide(subjectTotal, 1, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;
            vo.setSubjectAccuracy(subjectAccuracy);

            result.add(vo);
        }

        // Sort by subject accuracy ascending (weakest subject first)
        result.sort(Comparator.comparing(KpWeaknessVO::getSubjectAccuracy));
        return result;
    }

    /**
     * Get weakness data for a student filtered by subject
     */
    public KpWeaknessVO getStudentWeaknessBySubject(Long studentId, Long subjectId) {
        List<KpWeaknessVO> all = getStudentWeakness(studentId);
        return all.stream()
                .filter(v -> v.getSubjectId().equals(subjectId))
                .findFirst()
                .orElse(null);
    }

    private Map<Long, String> resolveSubjectNames(List<Long> subjectIds) {
        if (subjectIds.isEmpty()) return Collections.emptyMap();
        List<Map<String, Object>> rows = answerMapper.selectSubjectsByIds(subjectIds);
        Map<Long, String> map = new HashMap<>();
        for (Map<String, Object> row : rows) {
            Long id = ((Number) row.get("id")).longValue();
            String name = (String) row.get("name");
            map.put(id, name != null ? name : "未知");
        }
        return map;
    }

    // --- Internal helper classes ---

    private static class KpMapping {
        Long kpId;
        String kpName;
        Long subjectId;

        KpMapping(Long kpId, String kpName, Long subjectId) {
            this.kpId = kpId;
            this.kpName = kpName;
            this.subjectId = subjectId;
        }
    }

    private static class KpAgg {
        Long kpId;
        String kpName;
        Long subjectId;
        Long studentId;
        BigDecimal earnedScore = BigDecimal.ZERO;
        BigDecimal totalScore = BigDecimal.ZERO;
        int attemptCount;
    }
}
