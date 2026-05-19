package com.study.question.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
@TableName("knowledge_point")
public class KnowledgePoint {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long tenantId;
    private Long subjectId;
    private String name;
    private Long parentId;
    private Integer level;
    private Integer sort;
    private Integer status;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(exist = false)
    private List<KnowledgePoint> children;
}
