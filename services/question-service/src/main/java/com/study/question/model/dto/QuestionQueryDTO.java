package com.study.question.model.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class QuestionQueryDTO {
    private int page = 1;
    private int size = 10;
    private Long tenantId;
    private Long subjectId;
    private String type;
    private BigDecimal difficulty;
    private String status;
}
