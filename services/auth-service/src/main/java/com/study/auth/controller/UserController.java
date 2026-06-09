package com.study.auth.controller;

import com.study.auth.common.Result;
import com.study.auth.model.dto.PageResult;
import com.study.auth.model.dto.UserDTO;
import com.study.auth.model.entity.User;
import com.study.auth.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserService userService;

    @GetMapping("/page")
    public Result<PageResult<UserDTO>> page(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam Long tenantId) {
        return Result.ok(userService.page(page, size, tenantId));
    }

    @GetMapping("/{id}")
    public Result<UserDTO> getById(@PathVariable Long id) {
        return Result.ok(userService.getById(id));
    }

    @PostMapping
    public Result<?> create(@RequestBody User user) {
        userService.create(user);
        return Result.ok();
    }

    @PutMapping
    public Result<?> update(@RequestBody User user) {
        userService.update(user);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable Long id) {
        userService.delete(id);
        return Result.ok();
    }

    @PutMapping("/{id}/password")
    public Result<?> updatePassword(@PathVariable Long id, @RequestBody String newPassword) {
        userService.updatePassword(id, newPassword);
        return Result.ok();
    }

    @GetMapping("/{id}/roles")
    public Result<List<Long>> getUserRoles(@PathVariable Long id) {
        return Result.ok(userService.getUserRoleIds(id));
    }

    @PutMapping("/{id}/roles")
    public Result<?> assignRoles(@PathVariable Long id, @RequestBody Map<String, List<Long>> body) {
        userService.assignRoles(id, body.get("roleIds"));
        return Result.ok();
    }
}
