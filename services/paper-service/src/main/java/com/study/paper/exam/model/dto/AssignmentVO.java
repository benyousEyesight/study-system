package com.study.paper.exam.model.dto;

import lombok.Data;

@Data
public class AssignmentVO {
    private Long id;
    private Long examId;
    private String assignType;
    private Long assigneeId;
    private String assigneeName;
    private String assigneeTypeName;
}
