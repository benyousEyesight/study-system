package com.study.paper.exam.model.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
public class KpWeaknessVO {
    private Long subjectId;
    private String subjectName;
    private int totalKps;
    private BigDecimal subjectAccuracy;
    private List<KpItem> items;

    @Data
    public static class KpItem {
        private Long knowledgePointId;
        private String knowledgePointName;
        private BigDecimal accuracy;    // 0-100%
        private BigDecimal earnedScore;
        private BigDecimal totalScore;
        private int attemptCount;
        private String level;           // STRONG / MEDIUM / WEAK
    }
}
