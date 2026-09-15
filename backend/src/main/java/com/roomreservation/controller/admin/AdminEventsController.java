package com.roomreservation.controller.admin;

import com.roomreservation.common.Result;
import com.roomreservation.config.interceptor.RequireRole;
import com.roomreservation.entity.SysUser;
import com.roomreservation.service.RateLimitService;
import com.roomreservation.service.admin.AdminEventService;
import com.roomreservation.utils.TokenUtils;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理端事件概览接口：阶段 C 事件总线与投递状态
 */
@RestController
@RequestMapping("/api/admin/events")
public class AdminEventsController {

    @Resource
    private AdminEventService adminEventService;
    @Resource
    private RateLimitService rateLimitService;

    @RequireRole("admin")
    @GetMapping("/summary")
    public Result summary() {
        SysUser current = TokenUtils.getCurrentUser();
        rateLimitService.check("admin-events", current == null ? null : current.getId(), 30);
        return Result.success(adminEventService.summary());
    }
}
