package com.study.paper.model.dto;

import lombok.Data;

@Data
public class PaperQueryDTO {
    private int page = 1;
    private int size = 10;
    private Long tenantId;
    private Long subjectId;
    private String status;
    private String title;
}
