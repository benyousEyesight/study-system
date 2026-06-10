package com.study.auth.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.study.auth.mapper.GradeMapper;
import com.study.auth.model.entity.Grade;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class GradeService {

    @Autowired
    private GradeMapper gradeMapper;

    public List<Grade> list(Long tenantId) {
        return gradeMapper.selectList(
                new LambdaQueryWrapper<Grade>().eq(Grade::getTenantId, tenantId)
                        .orderByAsc(Grade::getSort));
    }

    public void create(Grade grade) {
        if (grade.getTenantId() == null) grade.setTenantId(0L);
        if (grade.getStatus() == null) grade.setStatus(1);
        gradeMapper.insert(grade);
    }

    public void update(Grade grade) {
        gradeMapper.updateById(grade);
    }

    public void delete(Long id) {
        gradeMapper.deleteById(id);
    }
}
