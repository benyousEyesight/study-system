package com.study.question.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.study.question.mapper.KnowledgePointMapper;
import com.study.question.model.entity.KnowledgePoint;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class KnowledgePointService {

    @Autowired
    private KnowledgePointMapper knowledgePointMapper;

    public List<KnowledgePoint> treeBySubject(Long subjectId, Long tenantId) {
        List<KnowledgePoint> all = knowledgePointMapper.selectList(
                new LambdaQueryWrapper<KnowledgePoint>()
                        .eq(KnowledgePoint::getSubjectId, subjectId)
                        .eq(KnowledgePoint::getTenantId, tenantId)
                        .orderByAsc(KnowledgePoint::getSort));
        return buildTree(all, 0L);
    }

    private List<KnowledgePoint> buildTree(List<KnowledgePoint> all, Long parentId) {
        return all.stream()
                .filter(kp -> kp.getParentId().equals(parentId))
                .peek(kp -> kp.setChildren(buildTree(all, kp.getId())))
                .collect(Collectors.toList());
    }

    public KnowledgePoint getById(Long id) {
        return knowledgePointMapper.selectById(id);
    }

    public void create(KnowledgePoint kp) {
        if (kp.getParentId() != null && kp.getParentId() > 0) {
            KnowledgePoint parent = knowledgePointMapper.selectById(kp.getParentId());
            kp.setLevel(parent != null ? parent.getLevel() + 1 : 1);
        } else {
            kp.setLevel(1);
        }
        knowledgePointMapper.insert(kp);
    }

    public void update(KnowledgePoint kp) {
        knowledgePointMapper.updateById(kp);
    }

    public void delete(Long id) {
        // 同时删除子节点
        List<KnowledgePoint> children = knowledgePointMapper.selectList(
                new LambdaQueryWrapper<KnowledgePoint>().eq(KnowledgePoint::getParentId, id));
        for (KnowledgePoint child : children) {
            knowledgePointMapper.deleteById(child.getId());
        }
        knowledgePointMapper.deleteById(id);
    }
}
