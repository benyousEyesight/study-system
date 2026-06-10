package com.study.paper.exam.model.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
public class PaperQualityVO {
    private Long examId;
    private Long paperId;
    private int totalQuestions;
    private int totalStudents;
    private BigDecimal difficultyIndex;
    private String difficultyLabel;
    private BigDecimal discriminationIndex;
    private String discriminationLabel;
    private BigDecimal reliabilityIndex;
    private String reliabilityLabel;
    private List<QuestionQuality> questions;

    @Data
    public static class QuestionQuality {
        private int sort;
        private Long questionId;
        private String type;
        private BigDecimal maxScore;
        private BigDecimal avgScore;
        private BigDecimal difficulty;
        private BigDecimal discrimination;
        private int highGroupCorrect;
        private int lowGroupCorrect;
    }
}
