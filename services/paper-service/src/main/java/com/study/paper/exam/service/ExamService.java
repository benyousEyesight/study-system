package com.study.paper.exam.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.study.paper.exam.mapper.ExamAssignmentMapper;
import com.study.paper.exam.mapper.ExamMapper;
import com.study.paper.exam.mapper.ExamSessionMapper;
import com.study.paper.exam.model.dto.*;
import com.study.paper.exam.model.entity.Exam;
import com.study.paper.exam.model.entity.ExamAssignment;
import com.study.paper.exam.model.entity.ExamSession;
import com.study.paper.mapper.PaperMapper;
import com.study.paper.model.entity.Paper;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ExamService {

    @Autowired
    private ExamMapper examMapper;
    @Autowired
    private ExamAssignmentMapper assignmentMapper;
    @Autowired
    private ExamSessionMapper sessionMapper;
    @Autowired
    private PaperMapper paperMapper;

    public Page<ExamVO> page(ExamQueryDTO query) {
        LambdaQueryWrapper<Exam> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(query.getTitle())) {
            wrapper.like(Exam::getTitle, query.getTitle());
        }
        if (StringUtils.hasText(query.getStatus())) {
            wrapper.eq(Exam::getStatus, query.getStatus());
        }
        if (query.getTenantId() != null) {
            wrapper.eq(Exam::getTenantId, query.getTenantId());
        }
        wrapper.orderByDesc(Exam::getCreatedAt);

        Page<Exam> p = examMapper.selectPage(new Page<>(query.getPage(), query.getSize()), wrapper);
        Page<ExamVO> voPage = new Page<>(p.getCurrent(), p.getSize(), p.getTotal());
        voPage.setRecords(p.getRecords().stream().map(this::toBasicVO).collect(Collectors.toList()));
        return voPage;
    }

    public ExamVO getById(Long id) {
        Exam exam = examMapper.selectById(id);
        if (exam == null) return null;
        ExamVO vo = toDetailVO(exam);

        long total = sessionMapper.selectCount(new LambdaQueryWrapper<ExamSession>().eq(ExamSession::getExamId, id));
        long graded = sessionMapper.selectCount(new LambdaQueryWrapper<ExamSession>()
                .eq(ExamSession::getExamId, id)
                .eq(ExamSession::getStatus, "GRADED"));
        vo.setTotalSessions((int) total);
        vo.setGradedSessions((int) graded);

        List<ExamAssignment> assigns = assignmentMapper.selectList(
                new LambdaQueryWrapper<ExamAssignment>().eq(ExamAssignment::getExamId, id));
        vo.setAssignments(assigns.stream().map(a -> {
            AssignmentVO avo = new AssignmentVO();
            BeanUtils.copyProperties(a, avo);
            return avo;
        }).collect(Collectors.toList()));

        return vo;
    }

    @Transactional
    public void create(ExamCreateDTO dto) {
        Exam exam = new Exam();
        BeanUtils.copyProperties(dto, exam);
        exam.setStatus(dto.getStatus() != null ? dto.getStatus() : "DRAFT");
        examMapper.insert(exam);

        if (dto.getAssignments() != null) {
            for (ExamCreateDTO.AssignmentItem item : dto.getAssignments()) {
                ExamAssignment assignment = new ExamAssignment();
                assignment.setExamId(exam.getId());
                assignment.setAssignType(item.getAssignType());
                assignment.setAssigneeId(item.getAssigneeId());
                assignmentMapper.insert(assignment);
            }
        }
    }

    @Transactional
    public void update(Long id, ExamCreateDTO dto) {
        Exam exam = examMapper.selectById(id);
        if (exam == null) return;
        BeanUtils.copyProperties(dto, exam);
        exam.setId(id);
        examMapper.updateById(exam);

        // 先删后插分配
        assignmentMapper.delete(new LambdaQueryWrapper<ExamAssignment>().eq(ExamAssignment::getExamId, id));
        if (dto.getAssignments() != null) {
            for (ExamCreateDTO.AssignmentItem item : dto.getAssignments()) {
                ExamAssignment assignment = new ExamAssignment();
                assignment.setExamId(id);
                assignment.setAssignType(item.getAssignType());
                assignment.setAssigneeId(item.getAssigneeId());
                assignmentMapper.insert(assignment);
            }
        }
    }

    @Transactional
    public void delete(Long id) {
        assignmentMapper.delete(new LambdaQueryWrapper<ExamAssignment>().eq(ExamAssignment::getExamId, id));
        examMapper.deleteById(id);
    }

    @Transactional
    public void updateStatus(Long id, String status) {
        Exam exam = new Exam();
        exam.setId(id);
        exam.setStatus(status);
        examMapper.updateById(exam);
    }

    @Transactional
    public void addAssignments(Long examId, AssignmentDTO dto) {
        if (dto.getUserIds() != null) {
            for (Long userId : dto.getUserIds()) {
                ExamAssignment a = new ExamAssignment();
                a.setExamId(examId); a.setAssignType("USER"); a.setAssigneeId(userId);
                assignmentMapper.insert(a);
            }
        }
        if (dto.getRoleIds() != null) {
            for (Long roleId : dto.getRoleIds()) {
                ExamAssignment a = new ExamAssignment();
                a.setExamId(examId); a.setAssignType("ROLE"); a.setAssigneeId(roleId);
                assignmentMapper.insert(a);
            }
        }
    }

    public List<AssignmentVO> getAssignments(Long examId) {
        List<ExamAssignment> list = assignmentMapper.selectList(
                new LambdaQueryWrapper<ExamAssignment>().eq(ExamAssignment::getExamId, examId));
        return list.stream().map(a -> {
            AssignmentVO vo = new AssignmentVO();
            BeanUtils.copyProperties(a, vo);
            return vo;
        }).collect(Collectors.toList());
    }

    @Transactional
    public void removeAssignment(Long examId, Long assignmentId) {
        assignmentMapper.deleteById(assignmentId);
    }

    private ExamVO toBasicVO(Exam exam) {
        ExamVO vo = new ExamVO();
        BeanUtils.copyProperties(exam, vo);
        if (exam.getPaperId() != null) {
            Paper paper = paperMapper.selectById(exam.getPaperId());
            if (paper != null) vo.setPaperTitle(paper.getTitle());
        }
        return vo;
    }

    private ExamVO toDetailVO(Exam exam) {
        return toBasicVO(exam);
    }
}
