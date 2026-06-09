package com.study.paper.model.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class GenerateDTO {
    @NotNull private Long templateId;
    private Long createdBy;
}
