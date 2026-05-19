package com.study.question.controller;

import com.study.question.common.Result;
import com.study.question.model.entity.Subject;
import com.study.question.service.SubjectService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/subjects")
public class SubjectController {
    @Autowired
    private SubjectService subjectService;

    @GetMapping("/list")
    public Result<List<Subject>> list(@RequestParam Long tenantId) {
        return Result.ok(subjectService.listByTenant(tenantId));
    }

    @GetMapping("/{id}")
    public Result<Subject> getById(@PathVariable Long id) {
        return Result.ok(subjectService.getById(id));
    }

    @PostMapping
    public Result<?> create(@RequestBody Subject subject) {
        subjectService.create(subject);
        return Result.ok();
    }

    @PutMapping
    public Result<?> update(@RequestBody Subject subject) {
        subjectService.update(subject);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable Long id) {
        subjectService.delete(id);
        return Result.ok();
    }
}
