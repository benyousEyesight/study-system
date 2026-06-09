package com.study.paper.exam.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class ExamCreateDTO {
    @NotNull private Long paperId;
    @NotBlank private String title;
    private String description;
    private String examCode;
    @NotBlank private String timeMode;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer durationMinutes;
    private Integer maxTabSwitches;
    private String antiCheatConfig;
    private String status;

    private List<AssignmentItem> assignments;

    @Data
    public static class AssignmentItem {
        @NotBlank private String assignType;
        private Long assigneeId;
    }
}
