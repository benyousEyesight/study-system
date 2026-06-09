package com.study.paper.exam.model.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
public class ExamSessionVO {
    private Long id;
    private Long examId;
    private Long userId;
    private String studentName;
    private LocalDateTime startTime;
    private LocalDateTime submittedAt;
    private Integer durationMinutes;
    private String status;
    private Integer tabSwitchCount;
    private BigDecimal totalScore;
    private LocalDateTime createdAt;

    // 考试页面数据（开始考试时返回）
    private ExamBasicVO examInfo;
    private List<SectionVO> sections;

    @Data
    public static class ExamBasicVO {
        private String title;
        private Integer durationMinutes;
        private Integer remainingSeconds;
        private Integer maxTabSwitches;
    }

    @Data
    public static class SectionVO {
        private Long id;
        private String title;
        private Integer sort;
        private Integer totalScore;
        private List<QuestionVO> questions;
    }

    @Data
    public static class QuestionVO {
        private Long questionId;
        private Integer sort;
        private Integer score;
        private String type;
        private Map<String, Object> content;
        private String answerSnapshot;
    }

    // 成绩查看时返回
    private List<AnswerResultVO> answers;

    @Data
    public static class AnswerResultVO {
        private Long questionId;
        private String questionType;
        private String questionContent;
        private String correctAnswer;
        private String studentAnswer;
        private BigDecimal score;
        private BigDecimal totalScore;
        private Integer isCorrect;
        private String gradingStatus;
        private String graderComment;
    }
}
