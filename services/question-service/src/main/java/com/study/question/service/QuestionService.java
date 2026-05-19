package com.study.question.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.study.question.mapper.QuestionMapper;
import com.study.question.mapper.QuestionKpMapper;
import com.study.question.model.dto.QuestionCreateDTO;
import com.study.question.model.dto.QuestionQueryDTO;
import com.study.question.model.dto.QuestionVO;
import com.study.question.model.entity.KnowledgePoint;
import com.study.question.model.entity.Question;
import com.study.question.model.entity.QuestionKp;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class QuestionService {

    @Autowired
    private QuestionMapper questionMapper;

    @Autowired
    private QuestionKpMapper questionKpMapper;

    public Page<QuestionVO> page(QuestionQueryDTO query) {
        LambdaQueryWrapper<Question> wrapper = new LambdaQueryWrapper<>();
        if (query.getSubjectId() != null) {
            wrapper.eq(Question::getSubjectId, query.getSubjectId());
        }
        if (StringUtils.hasText(query.getType())) {
            wrapper.eq(Question::getType, query.getType());
        }
        if (query.getDifficulty() != null) {
            wrapper.le(Question::getDifficulty, query.getDifficulty());
        }
        if (StringUtils.hasText(query.getStatus())) {
            wrapper.eq(Question::getStatus, query.getStatus());
        }
        if (query.getTenantId() != null) {
            wrapper.eq(Question::getTenantId, query.getTenantId());
        }
        wrapper.orderByDesc(Question::getCreatedAt);

        Page<Question> p = questionMapper.selectPage(new Page<>(query.getPage(), query.getSize()), wrapper);
        Page<QuestionVO> voPage = new Page<>(p.getCurrent(), p.getSize(), p.getTotal());
        voPage.setRecords(p.getRecords().stream().map(this::toVO).collect(Collectors.toList()));
        return voPage;
    }

    public QuestionVO getById(Long id) {
        Question q = questionMapper.selectById(id);
        if (q == null) return null;
        return toVO(q);
    }

    @Transactional
    public void create(QuestionCreateDTO dto) {
        Question q = new Question();
        BeanUtils.copyProperties(dto, q);
        questionMapper.insert(q);

        if (dto.getKnowledgePointIds() != null) {
            for (Long kpId : dto.getKnowledgePointIds()) {
                QuestionKp qkp = new QuestionKp();
                qkp.setQuestionId(q.getId());
                qkp.setKnowledgePointId(kpId);
                questionKpMapper.insert(qkp);
            }
        }
    }

    @Transactional
    public void update(Long id, QuestionCreateDTO dto) {
        Question q = questionMapper.selectById(id);
        if (q == null) return;
        BeanUtils.copyProperties(dto, q);
        q.setId(id);
        questionMapper.updateById(q);

        // 重新关联知识点
        questionKpMapper.delete(new LambdaQueryWrapper<QuestionKp>().eq(QuestionKp::getQuestionId, id));
        if (dto.getKnowledgePointIds() != null) {
            for (Long kpId : dto.getKnowledgePointIds()) {
                QuestionKp qkp = new QuestionKp();
                qkp.setQuestionId(id);
                qkp.setKnowledgePointId(kpId);
                questionKpMapper.insert(qkp);
            }
        }
    }

    public void delete(Long id) {
        questionMapper.deleteById(id);
        questionKpMapper.delete(new LambdaQueryWrapper<QuestionKp>().eq(QuestionKp::getQuestionId, id));
    }

    public void updateStatus(Long id, String status) {
        Question q = new Question();
        q.setId(id);
        q.setStatus(status);
        questionMapper.updateById(q);
    }

    private QuestionVO toVO(Question q) {
        QuestionVO vo = new QuestionVO();
        BeanUtils.copyProperties(q, vo);
        List<KnowledgePoint> kps = questionMapper.selectKnowledgePointsByQuestionId(q.getId());
        vo.setKnowledgePoints(kps);
        return vo;
    }
}
