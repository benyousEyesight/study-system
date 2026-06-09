package com.study.paper.exam.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("exam_assignment")
public class ExamAssignment {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long examId;
    private String assignType;
    private Long assigneeId;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
