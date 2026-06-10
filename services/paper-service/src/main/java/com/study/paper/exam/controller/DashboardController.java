package com.study.paper.exam.controller;

import com.study.paper.common.Result;
import com.study.paper.exam.model.dto.DashboardVO;
import com.study.paper.exam.service.DashboardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/stats/dashboard")
public class DashboardController {

    @Autowired
    private DashboardService dashboardService;

    @GetMapping("/teacher")
    public Result<DashboardVO> teacherDashboard(@RequestHeader("X-Tenant-Id") Long tenantId) {
        return Result.ok(dashboardService.getTeacherDashboard(tenantId));
    }
}
