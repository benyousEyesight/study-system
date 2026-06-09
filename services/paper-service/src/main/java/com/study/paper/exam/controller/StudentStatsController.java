package com.study.paper.exam.controller;

import com.study.paper.common.Result;
import com.study.paper.exam.model.dto.StudentStatsVO;
import com.study.paper.exam.service.StudentStatsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/stats/student")
public class StudentStatsController {

    @Autowired
    private StudentStatsService studentStatsService;

    @GetMapping("/overview")
    public Result<StudentStatsVO.Overview> overview(@RequestHeader("X-User-Id") Long userId) {
        return Result.ok(studentStatsService.getOverview(userId));
    }

    @GetMapping("/subjects")
    public Result<List<StudentStatsVO.SubjectStat>> subjects(@RequestHeader("X-User-Id") Long userId) {
        return Result.ok(studentStatsService.getSubjectStats(userId));
    }

    @GetMapping("/recent")
    public Result<List<StudentStatsVO.RecentExam>> recent(
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam(defaultValue = "10") int limit) {
        return Result.ok(studentStatsService.getRecentExams(userId, limit));
    }
}
