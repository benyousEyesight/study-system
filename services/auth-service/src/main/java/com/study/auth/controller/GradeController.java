package com.study.auth.controller;

import com.study.auth.common.Result;
import com.study.auth.model.entity.Grade;
import com.study.auth.service.GradeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/grades")
public class GradeController {

    @Autowired
    private GradeService gradeService;

    @GetMapping("/list")
    public Result<List<Grade>> list(@RequestParam Long tenantId) {
        return Result.ok(gradeService.list(tenantId));
    }

    @PostMapping
    public Result<?> create(@RequestBody Grade grade) {
        gradeService.create(grade);
        return Result.ok();
    }

    @PutMapping
    public Result<?> update(@RequestBody Grade grade) {
        gradeService.update(grade);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable Long id) {
        gradeService.delete(id);
        return Result.ok();
    }
}
