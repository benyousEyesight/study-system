package com.study.paper.exam.model.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
public class DashboardVO {
    private int totalExams;
    private int pendingGrading;
    private int publishedExams;
    private int totalSessions;
    private List<ExamCard> recentExams;
    private List<GradingAlert> gradingAlerts;

    @Data
    public static class ExamCard {
        private Long id;
        private String title;
        private String status;
        private int totalSessions;
        private int gradedSessions;
        private BigDecimal avgScore;
    }

    @Data
    public static class GradingAlert {
        private Long examId;
        private String examTitle;
        private int ungradedCount;
    }
}
