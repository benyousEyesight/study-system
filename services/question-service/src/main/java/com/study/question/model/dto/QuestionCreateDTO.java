package com.study.question.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
public class QuestionCreateDTO {
    @NotNull private Long tenantId;
    @NotNull private Long subjectId;
    @NotBlank private String type;
    private BigDecimal difficulty;
    @NotBlank private String contentJson;
    private String answerJson;
    private String analysis;
    private String status;
    private Long createdBy;
    private List<Long> knowledgePointIds;
}
