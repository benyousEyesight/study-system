package com.study.auth.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("clazz")
public class Clazz {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long tenantId;
    private Long gradeId;
    private String name;
    private Long headTeacherId;
    private Integer sort;
    private Integer status;
    @TableField(exist = false)
    private String gradeName;
    @TableField(exist = false)
    private String headTeacherName;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
