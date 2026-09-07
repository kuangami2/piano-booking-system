package com.roomreservation.controller.admin;

import com.roomreservation.common.Result;
import com.roomreservation.config.interceptor.RequireRole;
import com.roomreservation.service.admin.AdminFeedbackService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 管理端反馈接口：反馈列表与状态处理
 */
@RestController
@RequestMapping("/api/admin/feedback")
public class AdminFeedbackController {

    @Resource
    private AdminFeedbackService adminFeedbackService;

    @RequireRole("admin")
    @GetMapping
    public Result list(@RequestParam(required = false) String status,
                       @RequestParam(defaultValue = "1") Integer page,
                       @RequestParam(defaultValue = "10") Integer size) {
        return Result.success(adminFeedbackService.page(status, page, size));
    }

    @RequireRole("admin")
    @PutMapping("/{id}/status")
    public Result handle(@PathVariable Integer id, @RequestBody Map<String, Object> body) {
        Object raw = body.get("status");
        adminFeedbackService.handleStatus(id, raw == null ? null : raw.toString());
        return Result.success();
    }
}
