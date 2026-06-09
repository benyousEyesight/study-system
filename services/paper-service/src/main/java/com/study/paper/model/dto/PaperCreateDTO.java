package com.study.paper.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.util.List;

@Data
public class PaperCreateDTO {
    @NotNull private Long tenantId;
    @NotNull private Long subjectId;
    @NotBlank private String title;
    private Integer totalScore;
    private Integer durationMinutes;
    private String description;
    private String status;
    private Long createdBy;
    private List<SectionDTO> sections;

    @Data
    public static class SectionDTO {
        private Long id;
        @NotBlank private String title;
        private Integer sort;
        private Integer totalScore;
        private String description;
        private List<QuestionRefDTO> questions;
    }

    @Data
    public static class QuestionRefDTO {
        @NotNull private Long questionId;
        private Integer sort;
        @NotNull private Integer score;
    }
}
