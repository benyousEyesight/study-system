package com.study.paper.exam.controller;

import com.study.paper.common.Result;
import com.study.paper.exam.model.dto.*;
import com.study.paper.exam.service.GradingService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/grading")
public class GradingController {

    @Autowired
    private GradingService gradingService;

    @GetMapping("/exams")
    public Result<?> getGradingExams() {
        // 从请求头获取 tenantId，简化处理先传 0
        return Result.ok(gradingService.getGradingExams(0L));
    }

    @GetMapping("/exams/{examId}/sessions")
    public Result<?> getSessionsForGrading(@PathVariable Long examId) {
        return Result.ok(gradingService.getSessionsForGrading(examId, 0L));
    }

    @GetMapping("/sessions/{sessionId}")
    public Result<?> getSessionForGrading(@PathVariable Long sessionId) {
        return Result.ok(gradingService.getSessionForGrading(sessionId));
    }

    @PostMapping("/sessions/{sessionId}/grade")
    public Result<?> gradeSession(@PathVariable Long sessionId,
                                   @Valid @RequestBody GradeSubmitDTO dto) {
        gradingService.gradeSession(sessionId, 1L, dto);
        return Result.ok();
    }

    @PostMapping("/sessions/{sessionId}/release")
    public Result<?> releaseGrades(@PathVariable Long sessionId) {
        gradingService.releaseGrades(sessionId);
        return Result.ok();
    }
}
