package com.study.auth.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("teacher_assignment")
public class TeacherAssignment {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long clazzId;
    private Long teacherId;
    private Long subjectId;
    private String academicYear;
    @TableField(exist = false)
    private String teacherName;
    @TableField(exist = false)
    private String subjectName;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
