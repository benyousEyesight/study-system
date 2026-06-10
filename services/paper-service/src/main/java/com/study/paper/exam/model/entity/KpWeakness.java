package com.study.paper.exam.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("kp_weakness")
public class KpWeakness {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long tenantId;
    private Long studentId;
    private Long subjectId;
    private Long knowledgePointId;
    private String knowledgePointName;
    private BigDecimal totalScore;
    private BigDecimal earnedScore;
    private Integer attemptCount;
    private LocalDateTime computedAt;
}
