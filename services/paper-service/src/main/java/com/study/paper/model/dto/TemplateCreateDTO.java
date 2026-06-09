package com.study.paper.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
public class TemplateCreateDTO {
    @NotNull private Long tenantId;
    @NotNull private Long subjectId;
    @NotBlank private String name;
    private Integer totalScore;
    private String description;
    private String status;
    private Long createdBy;
    private List<RuleDTO> rules;

    @Data
    public static class RuleDTO {
        private Long id;
        @NotBlank private String sectionTitle;
        @NotBlank private String questionType;
        private BigDecimal difficulty;
        private String knowledgePointIds;
        @NotNull private Integer questionCount;
        @NotNull private Integer scorePerQuestion;
        private Integer sort;
    }
}
