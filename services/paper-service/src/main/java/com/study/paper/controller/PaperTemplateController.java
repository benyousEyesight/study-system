package com.study.paper.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.study.paper.common.Result;
import com.study.paper.model.dto.*;
import com.study.paper.service.PaperTemplateService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/paper-templates")
public class PaperTemplateController {
    @Autowired
    private PaperTemplateService templateService;

    @GetMapping("/page")
    public Result<Page<TemplateVO>> page(@Valid TemplateQueryDTO query) {
        return Result.ok(templateService.page(query));
    }

    @GetMapping("/{id}")
    public Result<TemplateVO> getById(@PathVariable Long id) {
        return Result.ok(templateService.getById(id));
    }

    @PostMapping
    public Result<?> create(@Valid @RequestBody TemplateCreateDTO dto) {
        templateService.create(dto);
        return Result.ok();
    }

    @PutMapping("/{id}")
    public Result<?> update(@PathVariable Long id, @Valid @RequestBody TemplateCreateDTO dto) {
        templateService.update(id, dto);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable Long id) {
        templateService.delete(id);
        return Result.ok();
    }

    @PostMapping("/generate")
    public Result<?> generate(@Valid @RequestBody GenerateDTO dto) {
        templateService.generate(dto);
        return Result.ok();
    }

    @PutMapping("/{id}/status")
    public Result<?> updateStatus(@PathVariable Long id, @RequestParam String status) {
        templateService.updateStatus(id, status);
        return Result.ok();
    }
}
