package com.study.paper.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("paper_template")
public class PaperTemplate {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long tenantId;
    private Long subjectId;
    private String name;
    private Integer totalScore;
    private String description;
    private String status;
    private Long createdBy;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
