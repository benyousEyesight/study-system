package com.study.question.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@TableName("question")
public class Question {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long tenantId;
    private Long subjectId;
    private String type;
    private BigDecimal difficulty;
    private String contentJson;
    private String answerJson;
    private String analysis;
    private String status;
    private Long createdBy;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
    @TableField(exist = false)
    private List<Long> knowledgePointIds;
}
