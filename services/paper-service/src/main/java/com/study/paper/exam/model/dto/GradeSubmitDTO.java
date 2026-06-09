package com.study.paper.exam.model.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
public class GradeSubmitDTO {
    @NotNull private List<GradeItem> grades;

    @Data
    public static class GradeItem {
        @NotNull private Long questionId;
        @NotNull private BigDecimal score;
        private String comment;
    }
}
