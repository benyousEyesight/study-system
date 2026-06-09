package com.study.paper.exam.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("exam_session")
public class ExamSession {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long examId;
    private Long userId;
    private String paperSnapshot;
    private LocalDateTime startTime;
    private LocalDateTime submittedAt;
    private Integer durationMinutes;
    private String status;
    private Integer tabSwitchCount;
    private BigDecimal totalScore;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
