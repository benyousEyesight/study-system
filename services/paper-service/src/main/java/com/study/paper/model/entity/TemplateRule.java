package com.study.paper.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("template_rule")
public class TemplateRule {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long templateId;
    private String sectionTitle;
    private String questionType;
    private BigDecimal difficulty;
    private String knowledgePointIds;
    private Integer questionCount;
    private Integer scorePerQuestion;
    private Integer sort;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
