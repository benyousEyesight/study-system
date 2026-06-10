package com.study.auth.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.study.auth.mapper.ClazzMapper;
import com.study.auth.mapper.GradeMapper;
import com.study.auth.mapper.StudentEnrollmentMapper;
import com.study.auth.mapper.TeacherAssignmentMapper;
import com.study.auth.mapper.UserMapper;
import com.study.auth.model.dto.PageResult;
import com.study.auth.model.entity.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ClazzService {

    @Autowired
    private ClazzMapper clazzMapper;
    @Autowired
    private GradeMapper gradeMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private StudentEnrollmentMapper enrollmentMapper;
    @Autowired
    private TeacherAssignmentMapper teacherAssignmentMapper;

    // --- 班级 CRUD ---

    public PageResult<Clazz> page(int pageNum, int size, Long tenantId, Long gradeId) {
        LambdaQueryWrapper<Clazz> q = new LambdaQueryWrapper<Clazz>()
                .eq(Clazz::getTenantId, tenantId)
                .orderByAsc(Clazz::getSort);
        if (gradeId != null && gradeId > 0) q.eq(Clazz::getGradeId, gradeId);
        Page<Clazz> p = clazzMapper.selectPage(new Page<>(pageNum, size), q);
        // 填充年级名和班主任名
        List<Clazz> records = p.getRecords();
        if (!records.isEmpty()) {
            List<Long> gradeIds = records.stream().map(Clazz::getGradeId).distinct().collect(Collectors.toList());
            Map<Long, String> gradeMap = gradeMapper.selectBatchIds(gradeIds).stream()
                    .collect(Collectors.toMap(Grade::getId, Grade::getName));
            List<Long> teacherIds = records.stream().map(Clazz::getHeadTeacherId)
                    .filter(id -> id != null && id > 0).distinct().collect(Collectors.toList());
            Map<Long, String> userMap = teacherIds.isEmpty() ? Map.of() : userMapper.selectBatchIds(teacherIds).stream()
                    .collect(Collectors.toMap(User::getId, u -> u.getRealName() != null ? u.getRealName() : u.getUsername()));
            for (Clazz c : records) {
                c.setGradeName(gradeMap.getOrDefault(c.getGradeId(), ""));
                if (c.getHeadTeacherId() != null) c.setHeadTeacherName(userMap.getOrDefault(c.getHeadTeacherId(), ""));
            }
        }
        return new PageResult<>(records, p.getTotal());
    }

    public List<Clazz> list(Long tenantId, Long gradeId) {
        LambdaQueryWrapper<Clazz> q = new LambdaQueryWrapper<Clazz>().eq(Clazz::getTenantId, tenantId);
        if (gradeId != null && gradeId > 0) q.eq(Clazz::getGradeId, gradeId);
        return clazzMapper.selectList(q.orderByAsc(Clazz::getSort));
    }

    public void create(Clazz clazz) {
        if (clazz.getTenantId() == null) clazz.setTenantId(0L);
        if (clazz.getStatus() == null) clazz.setStatus(1);
        clazzMapper.insert(clazz);
    }

    public void update(Clazz clazz) {
        clazzMapper.updateById(clazz);
    }

    @Transactional
    public void delete(Long id) {
        enrollmentMapper.delete(new LambdaQueryWrapper<StudentEnrollment>().eq(StudentEnrollment::getClazzId, id));
        teacherAssignmentMapper.delete(new LambdaQueryWrapper<TeacherAssignment>().eq(TeacherAssignment::getClazzId, id));
        clazzMapper.deleteById(id);
    }

    // --- 学生管理 ---

    public List<StudentEnrollment> getStudents(Long clazzId) {
        List<StudentEnrollment> list = enrollmentMapper.selectList(
                new LambdaQueryWrapper<StudentEnrollment>().eq(StudentEnrollment::getClazzId, clazzId));
        if (!list.isEmpty()) {
            List<Long> userIds = list.stream().map(StudentEnrollment::getStudentId).collect(Collectors.toList());
            Map<Long, User> userMap = userMapper.selectBatchIds(userIds).stream()
                    .collect(Collectors.toMap(User::getId, u -> u));
            for (StudentEnrollment e : list) {
                User u = userMap.get(e.getStudentId());
                if (u != null) {
                    e.setStudentName(u.getRealName() != null ? u.getRealName() : u.getUsername());
                    e.setStudentUsername(u.getUsername());
                }
            }
        }
        return list;
    }

    @Transactional
    public void addStudents(Long clazzId, List<Long> studentIds) {
        for (Long sid : studentIds) {
            long exists = enrollmentMapper.selectCount(
                    new LambdaQueryWrapper<StudentEnrollment>()
                            .eq(StudentEnrollment::getClazzId, clazzId)
                            .eq(StudentEnrollment::getStudentId, sid));
            if (exists == 0) {
                StudentEnrollment e = new StudentEnrollment();
                e.setClazzId(clazzId);
                e.setStudentId(sid);
                e.setStatus("ACTIVE");
                enrollmentMapper.insert(e);
            }
        }
    }

    public void removeStudent(Long id) {
        enrollmentMapper.deleteById(id);
    }

    // --- 教师管理 ---

    public List<TeacherAssignment> getTeachers(Long clazzId) {
        List<TeacherAssignment> list = teacherAssignmentMapper.selectList(
                new LambdaQueryWrapper<TeacherAssignment>().eq(TeacherAssignment::getClazzId, clazzId));
        if (!list.isEmpty()) {
            List<Long> userIds = list.stream().map(TeacherAssignment::getTeacherId).collect(Collectors.toList());
            Map<Long, User> userMap = userMapper.selectBatchIds(userIds).stream()
                    .collect(Collectors.toMap(User::getId, u -> u));
            for (TeacherAssignment ta : list) {
                User u = userMap.get(ta.getTeacherId());
                if (u != null) ta.setTeacherName(u.getRealName() != null ? u.getRealName() : u.getUsername());
            }
        }
        return list;
    }

    public void assignTeacher(TeacherAssignment ta) {
        teacherAssignmentMapper.insert(ta);
    }

    public void removeTeacher(Long id) {
        teacherAssignmentMapper.deleteById(id);
    }
}
