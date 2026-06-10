package com.study.auth.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("student_enrollment")
public class StudentEnrollment {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long clazzId;
    private Long studentId;
    private String academicYear;
    private String status;
    @TableField(exist = false)
    private String studentName;
    @TableField(exist = false)
    private String studentUsername;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
