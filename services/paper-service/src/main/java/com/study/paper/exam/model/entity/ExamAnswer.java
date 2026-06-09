package com.study.paper.exam.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("exam_answer")
public class ExamAnswer {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long sessionId;
    private Long sectionId;
    private Long questionId;
    private Long parentQuestionId;
    private String questionType;
    private String answerJson;
    private BigDecimal score;
    private Integer isCorrect;
    private Long graderId;
    private String graderComment;
    private String gradingStatus;
    private LocalDateTime answeredAt;
    private LocalDateTime gradedAt;
}
