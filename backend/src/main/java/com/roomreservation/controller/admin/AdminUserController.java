package com.roomreservation.controller.admin;

import com.roomreservation.common.Result;
import com.roomreservation.config.interceptor.RequireRole;
import com.roomreservation.service.admin.AdminUserService;
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
 * 管理端用户接口：用户列表、设会员、重置密码
 */
@RestController
@RequestMapping("/api/admin/users")
/**
 * 管理端用户接口：用户列表、会员设置与密码重置。
 */
public class AdminUserController {

    @Resource
    private AdminUserService adminUserService;

    @RequireRole("admin")
    @GetMapping
    public Result list(@RequestParam(required = false) String keyword,
                       @RequestParam(defaultValue = "1") Integer page,
                       @RequestParam(defaultValue = "10") Integer size) {
        return Result.success(adminUserService.pageUsers(keyword, page, size));
    }

    @RequireRole("admin")
    @PutMapping("/{id}/member")
    public Result setMember(@PathVariable Integer id, @RequestBody Map<String, Object> body) {
        adminUserService.setMember(id, toBoolean(body.get("isMember")));
        return Result.success();
    }

    @RequireRole("admin")
    @PutMapping("/{id}/password")
    public Result resetPassword(@PathVariable Integer id, @RequestBody Map<String, Object> body) {
        Object raw = body.get("newPassword");
        adminUserService.resetPassword(id, raw == null ? null : raw.toString());
        return Result.success();
    }

    private Boolean toBoolean(Object value) {
        if (value instanceof Boolean b) {
            return b;
        }
        if (value == null) {
            return null;
        }
        String s = value.toString().trim();
        if ("true".equalsIgnoreCase(s) || "1".equals(s)) {
            return Boolean.TRUE;
        }
        if ("false".equalsIgnoreCase(s) || "0".equals(s)) {
            return Boolean.FALSE;
        }
        return null;
    }
}
