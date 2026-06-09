package com.study.paper.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.study.paper.common.BusinessException;
import com.study.paper.mapper.*;
import com.study.paper.model.dto.*;
import com.study.paper.model.entity.*;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class PaperTemplateService {

    @Autowired
    private PaperTemplateMapper templateMapper;
    @Autowired
    private TemplateRuleMapper ruleMapper;
    @Autowired
    private PaperMapper paperMapper;
    @Autowired
    private PaperSectionMapper sectionMapper;
    @Autowired
    private PaperQuestionMapper paperQuestionMapper;
    @Autowired
    private ExternalQuestionRefMapper externalRefMapper;

    public Page<TemplateVO> page(TemplateQueryDTO query) {
        LambdaQueryWrapper<PaperTemplate> wrapper = new LambdaQueryWrapper<>();
        if (query.getSubjectId() != null) {
            wrapper.eq(PaperTemplate::getSubjectId, query.getSubjectId());
        }
        if (StringUtils.hasText(query.getName())) {
            wrapper.like(PaperTemplate::getName, query.getName());
        }
        if (StringUtils.hasText(query.getStatus())) {
            wrapper.eq(PaperTemplate::getStatus, query.getStatus());
        }
        if (query.getTenantId() != null) {
            wrapper.eq(PaperTemplate::getTenantId, query.getTenantId());
        }
        wrapper.orderByDesc(PaperTemplate::getCreatedAt);

        Page<PaperTemplate> p = templateMapper.selectPage(new Page<>(query.getPage(), query.getSize()), wrapper);
        Page<TemplateVO> voPage = new Page<>(p.getCurrent(), p.getSize(), p.getTotal());
        voPage.setRecords(p.getRecords().stream().map(this::toBasicVO).collect(Collectors.toList()));
        return voPage;
    }

    public TemplateVO getById(Long id) {
        PaperTemplate template = templateMapper.selectById(id);
        if (template == null) return null;
        return toDetailVO(template);
    }

    @Transactional
    public void create(TemplateCreateDTO dto) {
        PaperTemplate template = new PaperTemplate();
        BeanUtils.copyProperties(dto, template);
        template.setStatus(dto.getStatus() != null ? dto.getStatus() : "DRAFT");
        templateMapper.insert(template);

        if (dto.getRules() != null) {
            for (TemplateCreateDTO.RuleDTO ruleDTO : dto.getRules()) {
                TemplateRule rule = new TemplateRule();
                BeanUtils.copyProperties(ruleDTO, rule);
                rule.setTemplateId(template.getId());
                ruleMapper.insert(rule);
            }
        }
    }

    @Transactional
    public void update(Long id, TemplateCreateDTO dto) {
        PaperTemplate template = templateMapper.selectById(id);
        if (template == null) throw new BusinessException("模板不存在");

        BeanUtils.copyProperties(dto, template);
        template.setId(id);
        templateMapper.updateById(template);

        // 先删后插规则
        ruleMapper.delete(new LambdaQueryWrapper<TemplateRule>().eq(TemplateRule::getTemplateId, id));
        if (dto.getRules() != null) {
            for (TemplateCreateDTO.RuleDTO ruleDTO : dto.getRules()) {
                TemplateRule rule = new TemplateRule();
                BeanUtils.copyProperties(ruleDTO, rule);
                rule.setTemplateId(id);
                rule.setId(null);
                ruleMapper.insert(rule);
            }
        }
    }

    @Transactional
    public void delete(Long id) {
        ruleMapper.delete(new LambdaQueryWrapper<TemplateRule>().eq(TemplateRule::getTemplateId, id));
        templateMapper.deleteById(id);
    }

    @Transactional
    public void updateStatus(Long id, String status) {
        PaperTemplate template = new PaperTemplate();
        template.setId(id);
        template.setStatus(status);
        templateMapper.updateById(template);
    }

    @Transactional
    public void generate(GenerateDTO dto) {
        PaperTemplate template = templateMapper.selectById(dto.getTemplateId());
        if (template == null) throw new BusinessException("模板不存在");
        if (!"PUBLISHED".equals(template.getStatus())) {
            throw new BusinessException("只能从已发布的模板生成试卷");
        }

        List<TemplateRule> rules = ruleMapper.selectList(
                new LambdaQueryWrapper<TemplateRule>().eq(TemplateRule::getTemplateId, dto.getTemplateId())
                        .orderByAsc(TemplateRule::getSort));

        // 创建试卷
        Paper paper = new Paper();
        paper.setTenantId(template.getTenantId());
        paper.setSubjectId(template.getSubjectId());
        paper.setTitle(template.getName() + "_" + System.currentTimeMillis());
        paper.setTotalScore(template.getTotalScore());
        paper.setDescription(template.getDescription());
        paper.setStatus("DRAFT");
        paper.setCreatedBy(dto.getCreatedBy());
        paperMapper.insert(paper);

        int totalScore = 0;
        for (TemplateRule rule : rules) {
            // 解析知识点ID
            List<Long> kpIds = parseKpIds(rule.getKnowledgePointIds());

            // 查询匹配的题目
            List<Map<String, Object>> candidates = externalRefMapper.selectQuestionsForGenerate(
                    template.getSubjectId(), rule.getQuestionType(), rule.getDifficulty(),
                    kpIds.isEmpty() ? null : kpIds,
                    kpIds.isEmpty() ? 0 : kpIds.size());

            // 随机选取
            Collections.shuffle(candidates);
            int count = Math.min(rule.getQuestionCount(), candidates.size());
            List<Map<String, Object>> selected = candidates.subList(0, count);

            // 创建板块
            PaperSection section = new PaperSection();
            section.setPaperId(paper.getId());
            section.setTitle(rule.getSectionTitle());
            section.setSort(rule.getSort());
            section.setTotalScore(rule.getScorePerQuestion() * count);
            sectionMapper.insert(section);

            int sort = 0;
            for (Map<String, Object> q : selected) {
                PaperQuestion pq = new PaperQuestion();
                pq.setSectionId(section.getId());
                pq.setQuestionId((Long) q.get("id"));
                pq.setSort(sort++);
                pq.setScore(rule.getScorePerQuestion());
                paperQuestionMapper.insert(pq);
            }

            totalScore += rule.getScorePerQuestion() * count;
        }

        // 更新试卷总分
        paper.setTotalScore(totalScore);
        paperMapper.updateById(paper);
    }

    private List<Long> parseKpIds(String kpIdsJson) {
        if (!StringUtils.hasText(kpIdsJson)) return Collections.emptyList();
        // Simple JSON array parsing: [1,2,3]
        String trimmed = kpIdsJson.trim();
        if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
            String inner = trimmed.substring(1, trimmed.length() - 1);
            if (inner.isBlank()) return Collections.emptyList();
            return Arrays.stream(inner.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .map(Long::parseLong)
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

    private TemplateVO toBasicVO(PaperTemplate template) {
        TemplateVO vo = new TemplateVO();
        BeanUtils.copyProperties(template, vo);
        return vo;
    }

    private TemplateVO toDetailVO(PaperTemplate template) {
        TemplateVO vo = toBasicVO(template);
        List<TemplateRule> rules = ruleMapper.selectList(
                new LambdaQueryWrapper<TemplateRule>().eq(TemplateRule::getTemplateId, template.getId())
                        .orderByAsc(TemplateRule::getSort));
        vo.setRules(rules.stream().map(r -> {
            TemplateVO.RuleVO rvo = new TemplateVO.RuleVO();
            BeanUtils.copyProperties(r, rvo);
            return rvo;
        }).collect(Collectors.toList()));
        return vo;
    }
}
