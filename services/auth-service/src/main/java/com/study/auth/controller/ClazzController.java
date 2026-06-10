package com.study.auth.controller;

import com.study.auth.common.Result;
import com.study.auth.model.dto.PageResult;
import com.study.auth.model.entity.Clazz;
import com.study.auth.model.entity.StudentEnrollment;
import com.study.auth.model.entity.TeacherAssignment;
import com.study.auth.service.ClazzService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/clazzes")
public class ClazzController {

    @Autowired
    private ClazzService clazzService;

    @GetMapping("/page")
    public Result<PageResult<Clazz>> page(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam Long tenantId,
            @RequestParam(required = false) Long gradeId) {
        return Result.ok(clazzService.page(page, size, tenantId, gradeId));
    }

    @GetMapping("/list")
    public Result<List<Clazz>> list(@RequestParam Long tenantId, @RequestParam(required = false) Long gradeId) {
        return Result.ok(clazzService.list(tenantId, gradeId));
    }

    @PostMapping
    public Result<?> create(@RequestBody Clazz clazz) {
        clazzService.create(clazz);
        return Result.ok();
    }

    @PutMapping
    public Result<?> update(@RequestBody Clazz clazz) {
        clazzService.update(clazz);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable Long id) {
        clazzService.delete(id);
        return Result.ok();
    }

    // --- 学生管理 ---
    @GetMapping("/{clazzId}/students")
    public Result<List<StudentEnrollment>> getStudents(@PathVariable Long clazzId) {
        return Result.ok(clazzService.getStudents(clazzId));
    }

    @PostMapping("/{clazzId}/students")
    public Result<?> addStudents(@PathVariable Long clazzId, @RequestBody Map<String, List<Long>> body) {
        clazzService.addStudents(clazzId, body.get("studentIds"));
        return Result.ok();
    }

    @DeleteMapping("/{clazzId}/students/{id}")
    public Result<?> removeStudent(@PathVariable Long clazzId, @PathVariable Long id) {
        clazzService.removeStudent(id);
        return Result.ok();
    }

    // --- 教师管理 ---
    @GetMapping("/{clazzId}/teachers")
    public Result<List<TeacherAssignment>> getTeachers(@PathVariable Long clazzId) {
        return Result.ok(clazzService.getTeachers(clazzId));
    }

    @PostMapping("/{clazzId}/teachers")
    public Result<?> assignTeacher(@PathVariable Long clazzId, @RequestBody TeacherAssignment ta) {
        ta.setClazzId(clazzId);
        clazzService.assignTeacher(ta);
        return Result.ok();
    }

    @DeleteMapping("/{clazzId}/teachers/{id}")
    public Result<?> removeTeacher(@PathVariable Long clazzId, @PathVariable Long id) {
        clazzService.removeTeacher(id);
        return Result.ok();
    }
}
