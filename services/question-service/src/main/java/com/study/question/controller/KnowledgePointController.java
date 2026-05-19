package com.study.question.controller;

import com.study.question.common.Result;
import com.study.question.model.entity.KnowledgePoint;
import com.study.question.service.KnowledgePointService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/knowledge-points")
public class KnowledgePointController {
    @Autowired
    private KnowledgePointService knowledgePointService;

    @GetMapping("/tree")
    public Result<List<KnowledgePoint>> tree(@RequestParam Long subjectId, @RequestParam Long tenantId) {
        return Result.ok(knowledgePointService.treeBySubject(subjectId, tenantId));
    }

    @GetMapping("/{id}")
    public Result<KnowledgePoint> getById(@PathVariable Long id) {
        return Result.ok(knowledgePointService.getById(id));
    }

    @PostMapping
    public Result<?> create(@RequestBody KnowledgePoint kp) {
        knowledgePointService.create(kp);
        return Result.ok();
    }

    @PutMapping
    public Result<?> update(@RequestBody KnowledgePoint kp) {
        knowledgePointService.update(kp);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable Long id) {
        knowledgePointService.delete(id);
        return Result.ok();
    }
}
