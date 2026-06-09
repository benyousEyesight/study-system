package com.study.paper.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("paper_question")
public class PaperQuestion {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long sectionId;
    private Long questionId;
    private Integer sort;
    private Integer score;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
