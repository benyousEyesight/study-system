package com.study.paper.exam.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.study.paper.common.Result;
import com.study.paper.exam.model.dto.*;
import com.study.paper.exam.service.ExamService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/exams")
public class ExamController {

    @Autowired
    private ExamService examService;

    @GetMapping("/page")
    public Result<Page<ExamVO>> page(@Valid ExamQueryDTO query) {
        return Result.ok(examService.page(query));
    }

    @GetMapping("/{id}")
    public Result<ExamVO> getById(@PathVariable Long id) {
        return Result.ok(examService.getById(id));
    }

    @PostMapping
    public Result<Long> create(@Valid @RequestBody ExamCreateDTO dto) {
        Long id = examService.create(dto);
        return Result.ok(id);
    }

    @PutMapping("/{id}")
    public Result<?> update(@PathVariable Long id, @Valid @RequestBody ExamCreateDTO dto) {
        examService.update(id, dto);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable Long id) {
        examService.delete(id);
        return Result.ok();
    }

    @PutMapping("/{id}/status")
    public Result<?> updateStatus(@PathVariable Long id, @RequestParam String status) {
        examService.updateStatus(id, status);
        return Result.ok();
    }

    @PostMapping("/{id}/assignments")
    public Result<?> addAssignments(@PathVariable Long id, @Valid @RequestBody AssignmentDTO dto) {
        examService.addAssignments(id, dto);
        return Result.ok();
    }

    @GetMapping("/{id}/assignments")
    public Result<?> getAssignments(@PathVariable Long id) {
        return Result.ok(examService.getAssignments(id));
    }

    @DeleteMapping("/{examId}/assignments/{id}")
    public Result<?> removeAssignment(@PathVariable Long examId, @PathVariable Long id) {
        examService.removeAssignment(examId, id);
        return Result.ok();
    }
}
