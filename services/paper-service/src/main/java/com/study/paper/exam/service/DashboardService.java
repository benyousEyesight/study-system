package com.study.paper.exam.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.study.paper.exam.mapper.ExamMapper;
import com.study.paper.exam.mapper.ExamSessionMapper;
import com.study.paper.exam.model.dto.DashboardVO;
import com.study.paper.exam.model.entity.Exam;
import com.study.paper.exam.model.entity.ExamSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class DashboardService {

    @Autowired
    private ExamMapper examMapper;
    @Autowired
    private ExamSessionMapper sessionMapper;

    public DashboardVO getTeacherDashboard(Long tenantId) {
        List<Exam> allExams = examMapper.selectList(
                new LambdaQueryWrapper<Exam>()
                        .eq(Exam::getTenantId, tenantId)
                        .orderByDesc(Exam::getCreatedAt));

        DashboardVO vo = new DashboardVO();
        vo.setTotalExams(allExams.size());
        vo.setPublishedExams((int) allExams.stream().filter(e -> "PUBLISHED".equals(e.getStatus()) || "IN_PROGRESS".equals(e.getStatus())).count());

        // Recent 5 exams with stats
        List<DashboardVO.ExamCard> recent = allExams.stream().limit(5).map(e -> {
            DashboardVO.ExamCard card = new DashboardVO.ExamCard();
            card.setId(e.getId());
            card.setTitle(e.getTitle());
            card.setStatus(e.getStatus());

            long total = sessionMapper.selectCount(new LambdaQueryWrapper<ExamSession>()
                    .eq(ExamSession::getExamId, e.getId())
                    .in(ExamSession::getStatus, "SUBMITTED", "GRADING", "GRADED"));
            long graded = sessionMapper.selectCount(new LambdaQueryWrapper<ExamSession>()
                    .eq(ExamSession::getExamId, e.getId())
                    .eq(ExamSession::getStatus, "GRADED"));
            card.setTotalSessions((int) total);
            card.setGradedSessions((int) graded);

            // Avg score from graded sessions
            List<ExamSession> gradedSessions = sessionMapper.selectList(
                    new LambdaQueryWrapper<ExamSession>()
                            .eq(ExamSession::getExamId, e.getId())
                            .eq(ExamSession::getStatus, "GRADED"));
            if (!gradedSessions.isEmpty()) {
                BigDecimal sum = gradedSessions.stream()
                        .filter(s -> s.getTotalScore() != null)
                        .map(ExamSession::getTotalScore)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                card.setAvgScore(sum.divide(BigDecimal.valueOf(gradedSessions.size()), 1, RoundingMode.HALF_UP));
            }
            return card;
        }).collect(Collectors.toList());
        vo.setRecentExams(recent);

        // Grading alerts
        List<DashboardVO.GradingAlert> alerts = new ArrayList<>();
        for (Exam e : allExams) {
            long ungraded = sessionMapper.selectCount(new LambdaQueryWrapper<ExamSession>()
                    .eq(ExamSession::getExamId, e.getId())
                    .in(ExamSession::getStatus, "SUBMITTED", "GRADING"));
            if (ungraded > 0) {
                DashboardVO.GradingAlert alert = new DashboardVO.GradingAlert();
                alert.setExamId(e.getId());
                alert.setExamTitle(e.getTitle());
                alert.setUngradedCount((int) ungraded);
                alerts.add(alert);
            }
        }
        vo.setGradingAlerts(alerts);
        vo.setPendingGrading(alerts.stream().mapToInt(DashboardVO.GradingAlert::getUngradedCount).sum());

        long totalSessions = allExams.stream()
                .mapToLong(e -> sessionMapper.selectCount(new LambdaQueryWrapper<ExamSession>()
                        .eq(ExamSession::getExamId, e.getId())))
                .sum();
        vo.setTotalSessions((int) totalSessions);

        return vo;
    }
}
