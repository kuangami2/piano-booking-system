package com.roomreservation.controller.admin;

import com.roomreservation.common.Constants;
import com.roomreservation.common.Result;
import com.roomreservation.config.interceptor.RequireRole;
import com.roomreservation.entity.SysUser;
import com.roomreservation.exception.ServiceException;
import com.roomreservation.service.admin.AdminCreditService;
import com.roomreservation.utils.TokenUtils;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 管理端信用接口：信用流水、人工减分与恢复
 */
@RestController
@RequestMapping("/api/admin/credits")
public class AdminCreditController {

    @Resource
    private AdminCreditService adminCreditService;

    @RequireRole("admin")
    @GetMapping("/logs")
    public Result logs(@RequestParam(required = false) Integer userId,
                       @RequestParam(defaultValue = "1") Integer page,
                       @RequestParam(defaultValue = "10") Integer size) {
        return Result.success(adminCreditService.pageLogs(userId, page, size));
    }

    @RequireRole("admin")
    @PutMapping("/{userId}/deduct")
    public Result deduct(@PathVariable Integer userId, @RequestBody Map<String, Object> body) {
        Map<String, Object> data = adminCreditService.deduct(
                operatorId(), userId, toInt(body.get("points"), "points"), toStr(body.get("reason")));
        return Result.success(data);
    }

    @RequireRole("admin")
    @PutMapping("/{userId}/restore")
    public Result restore(@PathVariable Integer userId, @RequestBody Map<String, Object> body) {
        Map<String, Object> data = adminCreditService.restore(
                operatorId(), userId, toInt(body.get("points"), "points"), toStr(body.get("reason")));
        return Result.success(data);
    }

    private Integer operatorId() {
        SysUser user = TokenUtils.getCurrentUser();
        if (user == null) {
            throw new ServiceException(Constants.CODE_401, "未登录");
        }
        return user.getId();
    }

    private Integer toInt(Object value, String field) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.valueOf(value.toString().trim());
        } catch (NumberFormatException e) {
            throw new ServiceException(Constants.CODE_400, field + " 应为整数");
        }
    }

    private String toStr(Object value) {
        return value == null ? null : value.toString();
    }
}
