package com.study.paper.model.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class TemplateVO {
    private Long id;
    private Long tenantId;
    private Long subjectId;
    private String name;
    private Integer totalScore;
    private String description;
    private String status;
    private Long createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<RuleVO> rules;

    @Data
    public static class RuleVO {
        private Long id;
        private Long templateId;
        private String sectionTitle;
        private String questionType;
        private BigDecimal difficulty;
        private String knowledgePointIds;
        private Integer questionCount;
        private Integer scorePerQuestion;
        private Integer sort;
    }
}
