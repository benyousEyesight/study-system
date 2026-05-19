package com.study.auth.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.study.auth.common.BusinessException;
import com.study.auth.mapper.RoleMapper;
import com.study.auth.mapper.UserMapper;
import com.study.auth.model.dto.LoginRequest;
import com.study.auth.model.dto.LoginResponse;
import com.study.auth.model.dto.UserDTO;
import com.study.auth.model.entity.Permission;
import com.study.auth.model.entity.User;
import com.study.auth.security.JwtProvider;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class AuthService {

    @Autowired
    private UserMapper userMapper;
    @Autowired
    private RoleMapper roleMapper;
    @Autowired
    private JwtProvider jwtProvider;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired(required = false)
    private StringRedisTemplate redisTemplate;

    public LoginResponse login(LoginRequest request) {
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>()
                        .eq(User::getUsername, request.getUsername())
                        .eq(User::getTenantId, request.getTenantId() != null ? request.getTenantId() : 0));
        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new BusinessException(401, "用户名或密码错误");
        }
        if (user.getStatus() == 0) {
            throw new BusinessException(403, "账号已禁用");
        }

        String accessToken = jwtProvider.generateAccessToken(user.getId(), user.getTenantId(), user.getUserType());
        String refreshToken = jwtProvider.generateRefreshToken(user.getId());

        List<Permission> perms = roleMapper.selectPermissionsByUserId(user.getId());
        List<String> permCodes = perms.stream().map(Permission::getCode).collect(Collectors.toList());

        if (redisTemplate != null) {
            redisTemplate.opsForValue().set(
                    "auth:perms:" + user.getId(),
                    String.join(",", permCodes),
                    30, TimeUnit.MINUTES);
        }

        LoginResponse resp = new LoginResponse();
        resp.setAccessToken(accessToken);
        resp.setRefreshToken(refreshToken);
        resp.setUserInfo(buildUserDTO(user, permCodes));
        return resp;
    }

    private UserDTO buildUserDTO(User user, List<String> permissions) {
        UserDTO dto = new UserDTO();
        BeanUtils.copyProperties(user, dto);
        dto.setPermissions(permissions);
        return dto;
    }
}
