package com.study.question.model.dto;

import com.study.question.model.entity.KnowledgePoint;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class QuestionVO {
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
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<KnowledgePoint> knowledgePoints;
}
