package com.study.auth.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.study.auth.common.BusinessException;
import com.study.auth.mapper.RoleMapper;
import com.study.auth.mapper.UserMapper;
import com.study.auth.model.dto.PageResult;
import com.study.auth.model.entity.Role;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class RoleService {

    @Autowired
    private RoleMapper roleMapper;
    @Autowired
    private UserMapper userMapper;

    public PageResult<Role> page(int pageNum, int size, Long tenantId) {
        Page<Role> p = roleMapper.selectPage(new Page<>(pageNum, size),
                new LambdaQueryWrapper<Role>().eq(Role::getTenantId, tenantId));
        return new PageResult<>(p.getRecords(), p.getTotal());
    }

    public List<Role> listByTenant(Long tenantId) {
        return roleMapper.selectList(
                new LambdaQueryWrapper<Role>().eq(Role::getTenantId, tenantId));
    }

    public void create(Role role) {
        if (role.getTenantId() == null) {
            role.setTenantId(0L);
        }
        if (role.getStatus() == null) {
            role.setStatus(1);
        }
        if (role.getIsSystem() == null) {
            role.setIsSystem(0);
        }
        roleMapper.insert(role);
    }

    public void update(Role role) {
        roleMapper.updateById(role);
    }

    @Transactional
    public void delete(Long id) {
        Role role = roleMapper.selectById(id);
        if (role != null && role.getIsSystem() == 1) {
            throw new BusinessException("系统内置角色不能删除");
        }
        roleMapper.deleteById(id);
    }
}
