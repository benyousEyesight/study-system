package com.study.paper.exam.model.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
public class ExamReportVO {
    private Long examId;
    private String examTitle;
    private String paperTitle;
    private int totalStudents;
    private BigDecimal avgScore;
    private BigDecimal maxScore;
    private BigDecimal minScore;
    private BigDecimal passRate;
    private BigDecimal excellentRate;
    private BigDecimal fullScore;
    private ScoreDistribution distribution;
    private List<StudentRankItem> students;

    @Data
    public static class ScoreDistribution {
        private int below60;
        private int between60And69;
        private int between70And79;
        private int between80And89;
        private int between90And100;
    }

    @Data
    public static class StudentRankItem {
        private int rank;
        private Long userId;
        private BigDecimal totalScore;
        private String status;
    }
}
