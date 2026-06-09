package com.study.paper.exam.controller;

import com.study.paper.common.Result;
import com.study.paper.exam.model.dto.*;
import com.study.paper.exam.service.ExamSessionService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/my-exams")
public class MyExamController {

    @Autowired
    private ExamSessionService sessionService;

    @GetMapping
    public Result<?> getMyExams(@RequestHeader("X-User-Id") Long userId,
                                @RequestHeader("X-Tenant-Id") Long tenantId) {
        return Result.ok(sessionService.getMyExams(userId, tenantId, null));
    }

    @PostMapping("/{examId}/join")
    public Result<?> joinByCode(@PathVariable Long examId, @RequestParam String code,
                                @RequestHeader("X-User-Id") Long userId) {
        return Result.ok(sessionService.joinByCode(examId, code, userId));
    }

    @PostMapping("/{examId}/start")
    public Result<?> startExam(@PathVariable Long examId,
                               @RequestHeader("X-User-Id") Long userId) {
        return Result.ok(sessionService.startExam(examId, userId));
    }

    @GetMapping("/sessions/{sessionId}")
    public Result<?> getSessionData(@PathVariable Long sessionId,
                                    @RequestHeader("X-User-Id") Long userId) {
        return Result.ok(sessionService.getSessionData(sessionId, userId));
    }

    @PostMapping("/sessions/{sessionId}/answer")
    public Result<?> saveAnswer(@PathVariable Long sessionId,
                                @RequestHeader("X-User-Id") Long userId,
                                @Valid @RequestBody AnswerSubmitDTO dto) {
        sessionService.saveAnswer(sessionId, userId, dto);
        return Result.ok();
    }

    @PostMapping("/sessions/{sessionId}/submit")
    public Result<?> submitExam(@PathVariable Long sessionId,
                                @RequestHeader("X-User-Id") Long userId) {
        sessionService.submitExam(sessionId, userId);
        return Result.ok();
    }

    @GetMapping("/sessions/{sessionId}/result")
    public Result<?> getResult(@PathVariable Long sessionId,
                               @RequestHeader("X-User-Id") Long userId) {
        return Result.ok(sessionService.getResult(sessionId, userId));
    }

    @PostMapping("/sessions/{sessionId}/heartbeat")
    public Result<?> heartbeat(@PathVariable Long sessionId,
                               @RequestHeader("X-User-Id") Long userId,
                               @RequestParam int tabSwitchCount) {
        sessionService.heartbeat(sessionId, userId, tabSwitchCount);
        return Result.ok();
    }
}
