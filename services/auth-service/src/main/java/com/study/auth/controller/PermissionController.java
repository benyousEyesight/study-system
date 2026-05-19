package com.study.auth.controller;

import com.study.auth.common.Result;
import com.study.auth.mapper.PermissionMapper;
import com.study.auth.model.entity.Permission;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/permissions")
public class PermissionController {

    @Autowired
    private PermissionMapper permissionMapper;

    @GetMapping("/list")
    public Result<List<Permission>> list() {
        return Result.ok(permissionMapper.selectList(null));
    }
}
