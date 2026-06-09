package com.study.paper.model.dto;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class PaperVO {
    private Long id;
    private Long tenantId;
    private Long subjectId;
    private String title;
    private Integer totalScore;
    private Integer durationMinutes;
    private String description;
    private String status;
    private Long createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<SectionVO> sections;

    @Data
    public static class SectionVO {
        private Long id;
        private Long paperId;
        private String title;
        private Integer sort;
        private Integer totalScore;
        private String description;
        private List<QuestionRefVO> questions;
    }

    @Data
    public static class QuestionRefVO {
        private Long id;
        private Long sectionId;
        private Long questionId;
        private Integer sort;
        private Integer score;
        private QuestionBriefVO questionInfo;
    }

    @Data
    public static class QuestionBriefVO {
        private Long id;
        private String type;
        private String contentJson;
        private String difficulty;
    }
}
