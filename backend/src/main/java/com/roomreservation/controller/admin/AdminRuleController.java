package com.roomreservation.controller.admin;

import com.roomreservation.common.Result;
import com.roomreservation.config.interceptor.RequireRole;
import com.roomreservation.service.admin.AdminRuleService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 管理端规则接口：全量读取与批量更新，保存后即时生效
 */
@RestController
@RequestMapping("/api/admin/rules")
public class AdminRuleController {

    @Resource
    private AdminRuleService adminRuleService;

    @RequireRole("admin")
    @GetMapping
    public Result list() {
        return Result.success(adminRuleService.listAll());
    }

    @RequireRole("admin")
    @PutMapping
    public Result batchUpdate(@RequestBody List<Map<String, Object>> rules) {
        return Result.success(adminRuleService.batchUpdate(rules));
    }
}
