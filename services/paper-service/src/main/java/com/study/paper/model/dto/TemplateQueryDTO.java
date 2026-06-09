package com.study.paper.model.dto;

import lombok.Data;

@Data
public class TemplateQueryDTO {
    private int page = 1;
    private int size = 10;
    private Long tenantId;
    private Long subjectId;
    private String name;
    private String status;
}
