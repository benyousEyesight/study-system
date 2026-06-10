package com.study.paper.exam.controller;

import com.study.paper.common.Result;
import com.study.paper.exam.model.dto.ExamReportVO;
import com.study.paper.exam.service.ExamReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/stats/exam")
public class ExamReportController {

    @Autowired
    private ExamReportService examReportService;

    @GetMapping("/{examId}/report")
    public Result<ExamReportVO> getReport(@PathVariable Long examId) {
        ExamReportVO report = examReportService.getReport(examId);
        if (report == null) return Result.fail(404, "考试不存在");
        return Result.ok(report);
    }
}
