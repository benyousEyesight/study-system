package com.study.paper.exam.controller;

import com.study.paper.common.Result;
import com.study.paper.exam.model.dto.ExamReportVO;
import com.study.paper.exam.model.dto.PaperQualityVO;
import com.study.paper.exam.service.ExamExportService;
import com.study.paper.exam.service.ExamReportService;
import com.study.paper.exam.service.PaperQualityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/stats/exam")
public class ExamReportController {

    @Autowired
    private ExamReportService examReportService;
    @Autowired
    private ExamExportService examExportService;
    @Autowired
    private PaperQualityService paperQualityService;

    @GetMapping("/{examId}/report")
    public Result<ExamReportVO> getReport(@PathVariable Long examId) {
        ExamReportVO report = examReportService.getReport(examId);
        if (report == null) return Result.fail(404, "考试不存在");
        return Result.ok(report);
    }

    @GetMapping("/{examId}/quality")
    public Result<PaperQualityVO> getQuality(@PathVariable Long examId) throws Exception {
        PaperQualityVO q = paperQualityService.getQuality(examId);
        if (q == null) return Result.fail(404, "考试无有效数据");
        return Result.ok(q);
    }

    @GetMapping("/{examId}/export")
    public ResponseEntity<byte[]> exportExam(@PathVariable Long examId) throws Exception {
        var report = examReportService.getReport(examId);
        if (report == null) return ResponseEntity.notFound().build();

        byte[] excelBytes = examExportService.exportExamReport(examId);

        String filename = report.getExamTitle().replaceAll("[\\\\/:*?\"<>|]", "_") + "_考试报告.xlsx";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" +
                        new String(filename.getBytes("UTF-8"), "ISO-8859-1") + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(excelBytes);
    }
}
