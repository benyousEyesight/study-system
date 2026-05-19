package com.study.question.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.study.question.common.Result;
import com.study.question.model.dto.QuestionCreateDTO;
import com.study.question.model.dto.QuestionQueryDTO;
import com.study.question.model.dto.QuestionVO;
import com.study.question.service.QuestionService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/questions")
public class QuestionController {
    @Autowired
    private QuestionService questionService;

    @GetMapping("/page")
    public Result<Page<QuestionVO>> page(@Valid QuestionQueryDTO query) {
        return Result.ok(questionService.page(query));
    }

    @GetMapping("/{id}")
    public Result<QuestionVO> getById(@PathVariable Long id) {
        return Result.ok(questionService.getById(id));
    }

    @PostMapping
    public Result<?> create(@Valid @RequestBody QuestionCreateDTO dto) {
        questionService.create(dto);
        return Result.ok();
    }

    @PutMapping("/{id}")
    public Result<?> update(@PathVariable Long id, @RequestBody QuestionCreateDTO dto) {
        questionService.update(id, dto);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable Long id) {
        questionService.delete(id);
        return Result.ok();
    }

    @PutMapping("/{id}/status")
    public Result<?> updateStatus(@PathVariable Long id, @RequestParam String status) {
        questionService.updateStatus(id, status);
        return Result.ok();
    }
}
