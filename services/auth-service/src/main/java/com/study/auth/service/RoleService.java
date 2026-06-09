package com.study.auth.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.study.auth.common.BusinessException;
import com.study.auth.mapper.RoleMapper;
import com.study.auth.mapper.RolePermissionMapper;
import com.study.auth.mapper.UserMapper;
import com.study.auth.mapper.UserRoleMapper;
import com.study.auth.model.dto.PageResult;
import com.study.auth.model.entity.Role;
import com.study.auth.model.entity.RolePermission;
import com.study.auth.model.entity.UserRole;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class RoleService {

    @Autowired
    private RoleMapper roleMapper;
    @Autowired
    private RolePermissionMapper rolePermissionMapper;
    @Autowired
    private UserRoleMapper userRoleMapper;
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
        rolePermissionMapper.delete(new LambdaQueryWrapper<RolePermission>().eq(RolePermission::getRoleId, id));
        roleMapper.deleteById(id);
    }

    public List<Long> getUserIdsByRole(Long roleId) {
        return userRoleMapper.selectUserIdsByRoleId(roleId);
    }

    @Transactional
    public void assignUsers(Long roleId, List<Long> userIds) {
        userRoleMapper.delete(new LambdaQueryWrapper<UserRole>().eq(UserRole::getRoleId, roleId));
        if (userIds != null) {
            for (Long uid : userIds) {
                UserRole ur = new UserRole();
                ur.setRoleId(roleId);
                ur.setUserId(uid);
                userRoleMapper.insert(ur);
            }
        }
    }

    public List<Long> getPermissionIds(Long roleId) {
        return rolePermissionMapper.selectPermissionIdsByRoleId(roleId);
    }

    @Transactional
    public void assignPermissions(Long roleId, List<Long> permissionIds) {
        rolePermissionMapper.delete(new LambdaQueryWrapper<RolePermission>().eq(RolePermission::getRoleId, roleId));
        if (permissionIds != null) {
            for (Long pid : permissionIds) {
                RolePermission rp = new RolePermission();
                rp.setRoleId(roleId);
                rp.setPermissionId(pid);
                rolePermissionMapper.insert(rp);
            }
        }
    }
}
