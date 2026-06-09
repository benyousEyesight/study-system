package com.study.paper.exam.model.dto;

import lombok.Data;

@Data
public class ExamQueryDTO {
    private int page = 1;
    private int size = 10;
    private String title;
    private String status;
    private Long tenantId;
}
