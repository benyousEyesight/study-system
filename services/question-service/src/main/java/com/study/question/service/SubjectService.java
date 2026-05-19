package com.study.question.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.study.question.mapper.SubjectMapper;
import com.study.question.model.entity.Subject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class SubjectService {
    @Autowired
    private SubjectMapper subjectMapper;

    public List<Subject> listByTenant(Long tenantId) {
        return subjectMapper.selectList(
                new LambdaQueryWrapper<Subject>().eq(Subject::getTenantId, tenantId));
    }

    public Subject getById(Long id) {
        return subjectMapper.selectById(id);
    }

    public void create(Subject subject) {
        subjectMapper.insert(subject);
    }

    public void update(Subject subject) {
        subjectMapper.updateById(subject);
    }

    public void delete(Long id) {
        subjectMapper.deleteById(id);
    }
}
