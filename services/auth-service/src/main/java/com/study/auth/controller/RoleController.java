package com.study.auth.controller;

import com.study.auth.common.Result;
import com.study.auth.model.dto.PageResult;
import com.study.auth.model.entity.Role;
import com.study.auth.service.RoleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/roles")
public class RoleController {

    @Autowired
    private RoleService roleService;

    @GetMapping("/page")
    public Result<PageResult<Role>> page(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam Long tenantId) {
        return Result.ok(roleService.page(page, size, tenantId));
    }

    @GetMapping("/list")
    public Result<List<Role>> list(@RequestParam Long tenantId) {
        return Result.ok(roleService.listByTenant(tenantId));
    }

    @PostMapping
    public Result<?> create(@RequestBody Role role) {
        roleService.create(role);
        return Result.ok();
    }

    @PutMapping
    public Result<?> update(@RequestBody Role role) {
        roleService.update(role);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable Long id) {
        roleService.delete(id);
        return Result.ok();
    }

    @GetMapping("/{id}/permissions")
    public Result<List<Long>> getPermissions(@PathVariable Long id) {
        return Result.ok(roleService.getPermissionIds(id));
    }

    @PutMapping("/{id}/permissions")
    public Result<?> assignPermissions(@PathVariable Long id, @RequestBody Map<String, List<Long>> body) {
        roleService.assignPermissions(id, body.get("permissionIds"));
        return Result.ok();
    }

    @GetMapping("/{id}/users")
    public Result<List<Long>> getUsers(@PathVariable Long id) {
        return Result.ok(roleService.getUserIdsByRole(id));
    }

    @PutMapping("/{id}/users")
    public Result<?> assignUsers(@PathVariable Long id, @RequestBody Map<String, List<Long>> body) {
        roleService.assignUsers(id, body.get("userIds"));
        return Result.ok();
    }
}
