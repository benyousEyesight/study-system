package com.study.paper.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("paper_section")
public class PaperSection {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long paperId;
    private String title;
    private Integer sort;
    private Integer totalScore;
    private String description;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
