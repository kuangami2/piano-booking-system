package com.roomreservation.controller.admin;

import com.roomreservation.common.Result;
import com.roomreservation.config.interceptor.RequireRole;
import com.roomreservation.service.admin.AdminStatsService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理端统计接口：首页概览与琴房使用率明细
 */
@RestController
@RequestMapping("/api/admin/stats")
/**
 * 管理端统计接口：首页概览与琴房使用率明细。
 */
public class AdminStatsController {

    @Resource
    private AdminStatsService adminStatsService;

    @RequireRole("admin")
    @GetMapping("/overview")
    public Result overview() {
        return Result.success(adminStatsService.overview());
    }

    @RequireRole("admin")
    @GetMapping("/usage")
    public Result usage(@RequestParam String from, @RequestParam String to) {
        return Result.success(adminStatsService.usage(from, to));
    }
}
