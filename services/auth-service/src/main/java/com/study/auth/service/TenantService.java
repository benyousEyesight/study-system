package com.study.auth.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.study.auth.mapper.TenantMapper;
import com.study.auth.model.dto.PageResult;
import com.study.auth.model.entity.Tenant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class TenantService {

    @Autowired
    private TenantMapper tenantMapper;

    public PageResult<Tenant> page(int pageNum, int size) {
        Page<Tenant> p = tenantMapper.selectPage(new Page<>(pageNum, size), null);
        return new PageResult<>(p.getRecords(), p.getTotal());
    }

    public Tenant getById(Long id) {
        return tenantMapper.selectById(id);
    }

    public void create(Tenant tenant) {
        tenantMapper.insert(tenant);
    }

    public void update(Tenant tenant) {
        tenantMapper.updateById(tenant);
    }

    public void delete(Long id) {
        tenantMapper.deleteById(id);
    }
}
