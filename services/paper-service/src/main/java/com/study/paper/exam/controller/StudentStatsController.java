package com.study.paper.exam.controller;

import com.study.paper.common.Result;
import com.study.paper.exam.model.dto.KpWeaknessVO;
import com.study.paper.exam.model.dto.StudentStatsVO;
import com.study.paper.exam.service.KpWeaknessService;
import com.study.paper.exam.service.StudentStatsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/stats/student")
public class StudentStatsController {

    @Autowired
    private StudentStatsService studentStatsService;
    @Autowired
    private KpWeaknessService kpWeaknessService;

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

    @GetMapping("/weakness")
    public Result<List<KpWeaknessVO>> weakness(@RequestHeader("X-User-Id") Long userId) {
        return Result.ok(kpWeaknessService.getStudentWeakness(userId));
    }

    @GetMapping("/weakness/subject/{subjectId}")
    public Result<KpWeaknessVO> weaknessBySubject(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long subjectId) {
        return Result.ok(kpWeaknessService.getStudentWeaknessBySubject(userId, subjectId));
    }

    @PostMapping("/weakness/compute")
    public Result<Void> compute(@RequestHeader("X-User-Id") Long userId) {
        kpWeaknessService.computeForStudent(userId);
        return Result.ok();
    }
}
