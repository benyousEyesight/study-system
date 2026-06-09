package com.study.paper.exam.model.dto;

import lombok.Data;
import java.util.List;

@Data
public class AssignmentDTO {
    private List<Long> userIds;
    private List<Long> roleIds;
    private String examCode;
}
