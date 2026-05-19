package com.study.auth.controller;

import com.study.auth.common.Result;
import com.study.auth.model.dto.LoginRequest;
import com.study.auth.model.dto.LoginResponse;
import com.study.auth.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return Result.ok(authService.login(request));
    }

    @PostMapping("/logout")
    public Result<?> logout() {
        return Result.ok();
    }
}
