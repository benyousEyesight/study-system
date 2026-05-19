package com.study.auth.controller;

import com.study.auth.common.Result;
import com.study.auth.model.dto.PageResult;
import com.study.auth.model.entity.Tenant;
import com.study.auth.service.TenantService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/tenants")
public class TenantController {

    @Autowired
    private TenantService tenantService;

    @GetMapping("/page")
    public Result<PageResult<Tenant>> page(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return Result.ok(tenantService.page(page, size));
    }

    @GetMapping("/{id}")
    public Result<Tenant> getById(@PathVariable Long id) {
        return Result.ok(tenantService.getById(id));
    }

    @PostMapping
    public Result<?> create(@RequestBody Tenant tenant) {
        tenantService.create(tenant);
        return Result.ok();
    }

    @PutMapping
    public Result<?> update(@RequestBody Tenant tenant) {
        tenantService.update(tenant);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable Long id) {
        tenantService.delete(id);
        return Result.ok();
    }
}
