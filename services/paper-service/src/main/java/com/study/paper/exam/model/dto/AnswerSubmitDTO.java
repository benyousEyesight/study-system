package com.study.paper.exam.model.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AnswerSubmitDTO {
    @NotNull private Long questionId;
    private Long parentQuestionId;
    private Long sectionId;
    private String answerJson;
}
