package com.study.paper.exam.model.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class StudentStatsVO {

    @Data
    public static class Overview {
        private int totalExams;
        private BigDecimal avgScore;
        private BigDecimal bestScore;
        private BigDecimal passRate;
        private int totalGraded;
    }

    @Data
    public static class SubjectStat {
        private Long subjectId;
        private String subjectName;
        private int totalQuestions;
        private int correctCount;
        private BigDecimal accuracy;
    }

    @Data
    public static class RecentExam {
        private Long examId;
        private String examTitle;
        private BigDecimal totalScore;
        private int rank;
        private int totalStudents;
        private LocalDateTime submittedAt;
    }
}
