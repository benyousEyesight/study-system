package com.study.paper.exam.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("paper_quality")
public class PaperQuality {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long examId;
    private Long paperId;
    private BigDecimal difficultyIndex;
    private BigDecimal discriminationIndex;
    private BigDecimal reliabilityIndex;
    private Integer totalQuestions;
    private Integer totalStudents;
    private String details;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
