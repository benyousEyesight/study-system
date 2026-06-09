package com.study.paper.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.study.paper.common.Result;
import com.study.paper.model.dto.PaperCreateDTO;
import com.study.paper.model.dto.PaperQueryDTO;
import com.study.paper.model.dto.PaperVO;
import com.study.paper.service.PaperService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/papers")
public class PaperController {
    @Autowired
    private PaperService paperService;

    @GetMapping("/page")
    public Result<Page<PaperVO>> page(@Valid PaperQueryDTO query) {
        return Result.ok(paperService.page(query));
    }

    @GetMapping("/{id}")
    public Result<PaperVO> getById(@PathVariable Long id) {
        return Result.ok(paperService.getById(id));
    }

    @PostMapping
    public Result<?> create(@Valid @RequestBody PaperCreateDTO dto) {
        paperService.create(dto);
        return Result.ok();
    }

    @PutMapping("/{id}")
    public Result<?> update(@PathVariable Long id, @Valid @RequestBody PaperCreateDTO dto) {
        paperService.update(id, dto);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable Long id) {
        paperService.delete(id);
        return Result.ok();
    }

    @PutMapping("/{id}/status")
    public Result<?> updateStatus(@PathVariable Long id, @RequestParam String status) {
        paperService.updateStatus(id, status);
        return Result.ok();
    }
}
