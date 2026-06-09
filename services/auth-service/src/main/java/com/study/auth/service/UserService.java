package com.study.auth.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.study.auth.common.BusinessException;
import com.study.auth.mapper.UserMapper;
import com.study.auth.model.dto.PageResult;
import com.study.auth.model.dto.UserDTO;
import com.study.auth.model.entity.User;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserService {

    @Autowired
    private UserMapper userMapper;
    @Autowired
    private PasswordEncoder passwordEncoder;

    public PageResult<UserDTO> page(int pageNum, int size, Long tenantId) {
        Page<User> p = userMapper.selectPage(new Page<>(pageNum, size),
                new LambdaQueryWrapper<User>().eq(User::getTenantId, tenantId));
        List<UserDTO> list = p.getRecords().stream().map(this::buildDTO).collect(Collectors.toList());
        return new PageResult<>(list, p.getTotal());
    }

    public UserDTO getById(Long id) {
        User user = userMapper.selectById(id);
        return buildDTO(user);
    }

    public void create(User user) {
        if (user.getPasswordHash() == null) {
            user.setPasswordHash(user.getPassword());
        }
        if (user.getPasswordHash() == null || user.getPasswordHash().isBlank()) {
            throw new BusinessException(400, "密码不能为空");
        }
        user.setPasswordHash(passwordEncoder.encode(user.getPasswordHash()));
        userMapper.insert(user);
    }

    public void update(User user) {
        user.setPasswordHash(null);
        userMapper.updateById(user);
    }

    public void updatePassword(Long id, String newPassword) {
        User user = new User();
        user.setId(id);
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userMapper.updateById(user);
    }

    public void delete(Long id) {
        userMapper.deleteById(id);
    }

    private UserDTO buildDTO(User user) {
        if (user == null) return null;
        UserDTO dto = new UserDTO();
        BeanUtils.copyProperties(user, dto);
        return dto;
    }
}
