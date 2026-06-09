package com.study.paper.exam.model.dto;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class ExamVO {
    private Long id;
    private Long tenantId;
    private Long paperId;
    private String paperTitle;
    private String title;
    private String description;
    private String examCode;
    private String timeMode;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer durationMinutes;
    private Integer maxTabSwitches;
    private String antiCheatConfig;
    private String status;
    private Long createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private List<AssignmentVO> assignments;
    private Integer totalSessions;
    private Integer gradedSessions;
}
