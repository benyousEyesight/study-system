package com.study.paper.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.study.paper.common.BusinessException;
import com.study.paper.mapper.ExternalQuestionRefMapper;
import com.study.paper.mapper.PaperMapper;
import com.study.paper.mapper.PaperQuestionMapper;
import com.study.paper.mapper.PaperSectionMapper;
import com.study.paper.model.dto.PaperCreateDTO;
import com.study.paper.model.dto.PaperQueryDTO;
import com.study.paper.model.dto.PaperVO;
import com.study.paper.model.entity.Paper;
import com.study.paper.model.entity.PaperQuestion;
import com.study.paper.model.entity.PaperSection;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class PaperService {

    @Autowired
    private PaperMapper paperMapper;
    @Autowired
    private PaperSectionMapper sectionMapper;
    @Autowired
    private PaperQuestionMapper paperQuestionMapper;
    @Autowired
    private ExternalQuestionRefMapper externalRefMapper;

    public Page<PaperVO> page(PaperQueryDTO query) {
        LambdaQueryWrapper<Paper> wrapper = new LambdaQueryWrapper<>();
        if (query.getSubjectId() != null) {
            wrapper.eq(Paper::getSubjectId, query.getSubjectId());
        }
        if (StringUtils.hasText(query.getStatus())) {
            wrapper.eq(Paper::getStatus, query.getStatus());
        }
        if (StringUtils.hasText(query.getTitle())) {
            wrapper.like(Paper::getTitle, query.getTitle());
        }
        if (query.getTenantId() != null) {
            wrapper.eq(Paper::getTenantId, query.getTenantId());
        }
        wrapper.orderByDesc(Paper::getCreatedAt);

        Page<Paper> p = paperMapper.selectPage(new Page<>(query.getPage(), query.getSize()), wrapper);
        Page<PaperVO> voPage = new Page<>(p.getCurrent(), p.getSize(), p.getTotal());
        voPage.setRecords(p.getRecords().stream().map(this::toBasicVO).collect(Collectors.toList()));
        return voPage;
    }

    public PaperVO getById(Long id) {
        Paper paper = paperMapper.selectById(id);
        if (paper == null) return null;
        return toDetailVO(paper);
    }

    @Transactional
    public void create(PaperCreateDTO dto) {
        Paper paper = new Paper();
        BeanUtils.copyProperties(dto, paper);
        paper.setStatus(dto.getStatus() != null ? dto.getStatus() : "DRAFT");
        paperMapper.insert(paper);

        if (dto.getSections() != null) {
            for (PaperCreateDTO.SectionDTO sectionDTO : dto.getSections()) {
                PaperSection section = new PaperSection();
                BeanUtils.copyProperties(sectionDTO, section);
                section.setPaperId(paper.getId());
                sectionMapper.insert(section);

                if (sectionDTO.getQuestions() != null) {
                    for (PaperCreateDTO.QuestionRefDTO qRef : sectionDTO.getQuestions()) {
                        PaperQuestion pq = new PaperQuestion();
                        pq.setSectionId(section.getId());
                        pq.setQuestionId(qRef.getQuestionId());
                        pq.setSort(qRef.getSort() != null ? qRef.getSort() : 0);
                        pq.setScore(qRef.getScore());
                        paperQuestionMapper.insert(pq);
                    }
                }
            }
        }
    }

    @Transactional
    public void update(Long id, PaperCreateDTO dto) {
        Paper paper = paperMapper.selectById(id);
        if (paper == null) throw new BusinessException("试卷不存在");

        BeanUtils.copyProperties(dto, paper);
        paper.setId(id);
        paperMapper.updateById(paper);

        // 先删后插：删除原有板块和题目关联
        List<PaperSection> oldSections = sectionMapper.selectList(
                new LambdaQueryWrapper<PaperSection>().eq(PaperSection::getPaperId, id));
        for (PaperSection s : oldSections) {
            paperQuestionMapper.delete(new LambdaQueryWrapper<PaperQuestion>().eq(PaperQuestion::getSectionId, s.getId()));
        }
        sectionMapper.delete(new LambdaQueryWrapper<PaperSection>().eq(PaperSection::getPaperId, id));

        // 重新插入
        if (dto.getSections() != null) {
            for (PaperCreateDTO.SectionDTO sectionDTO : dto.getSections()) {
                PaperSection section = new PaperSection();
                BeanUtils.copyProperties(sectionDTO, section);
                section.setPaperId(id);
                section.setId(null);
                sectionMapper.insert(section);

                if (sectionDTO.getQuestions() != null) {
                    for (PaperCreateDTO.QuestionRefDTO qRef : sectionDTO.getQuestions()) {
                        PaperQuestion pq = new PaperQuestion();
                        pq.setSectionId(section.getId());
                        pq.setQuestionId(qRef.getQuestionId());
                        pq.setSort(qRef.getSort() != null ? qRef.getSort() : 0);
                        pq.setScore(qRef.getScore());
                        paperQuestionMapper.insert(pq);
                    }
                }
            }
        }
    }

    @Transactional
    public void delete(Long id) {
        List<PaperSection> sections = sectionMapper.selectList(
                new LambdaQueryWrapper<PaperSection>().eq(PaperSection::getPaperId, id));
        for (PaperSection s : sections) {
            paperQuestionMapper.delete(new LambdaQueryWrapper<PaperQuestion>().eq(PaperQuestion::getSectionId, s.getId()));
        }
        sectionMapper.delete(new LambdaQueryWrapper<PaperSection>().eq(PaperSection::getPaperId, id));
        paperMapper.deleteById(id);
    }

    @Transactional
    public void updateStatus(Long id, String status) {
        Paper paper = new Paper();
        paper.setId(id);
        paper.setStatus(status);
        paperMapper.updateById(paper);
    }

    private PaperVO toBasicVO(Paper paper) {
        PaperVO vo = new PaperVO();
        BeanUtils.copyProperties(paper, vo);
        return vo;
    }

    private PaperVO toDetailVO(Paper paper) {
        PaperVO vo = toBasicVO(paper);

        List<PaperSection> sections = sectionMapper.selectList(
                new LambdaQueryWrapper<PaperSection>().eq(PaperSection::getPaperId, paper.getId())
                        .orderByAsc(PaperSection::getSort));

        vo.setSections(sections.stream().map(s -> {
            PaperVO.SectionVO svo = new PaperVO.SectionVO();
            BeanUtils.copyProperties(s, svo);

            List<PaperQuestion> pqs = paperQuestionMapper.selectList(
                    new LambdaQueryWrapper<PaperQuestion>().eq(PaperQuestion::getSectionId, s.getId())
                            .orderByAsc(PaperQuestion::getSort));

            svo.setQuestions(pqs.stream().map(pq -> {
                PaperVO.QuestionRefVO qvo = new PaperVO.QuestionRefVO();
                BeanUtils.copyProperties(pq, qvo);

                Map<String, Object> qInfo = externalRefMapper.selectQuestionById(pq.getQuestionId());
                if (qInfo != null) {
                    PaperVO.QuestionBriefVO brief = new PaperVO.QuestionBriefVO();
                    brief.setId((Long) qInfo.get("id"));
                    brief.setType((String) qInfo.get("type"));
                    brief.setContentJson((String) qInfo.get("contentJson"));
                    brief.setDifficulty(String.valueOf(qInfo.get("difficulty")));
                    qvo.setQuestionInfo(brief);
                }
                return qvo;
            }).collect(Collectors.toList()));

            return svo;
        }).collect(Collectors.toList()));

        return vo;
    }
}
