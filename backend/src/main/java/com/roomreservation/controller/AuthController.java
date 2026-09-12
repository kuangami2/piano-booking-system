package com.roomreservation.controller;

import com.roomreservation.common.Constants;
import com.roomreservation.common.Result;
import com.roomreservation.config.interceptor.AuthAccess;
import com.roomreservation.dto.ChangePasswordRequest;
import com.roomreservation.dto.ForgotPasswordRequest;
import com.roomreservation.dto.LoginRequest;
import com.roomreservation.dto.RegisterRequest;
import com.roomreservation.dto.ResetPasswordRequest;
import com.roomreservation.entity.SysUser;
import com.roomreservation.exception.ServiceException;
import com.roomreservation.service.ActivityService;
import com.roomreservation.service.ISysUserService;
import com.roomreservation.service.RateLimitService;
import com.roomreservation.utils.TokenUtils;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 鉴权接口，注册、登录、个人信息、密码修改与找回
 */
@RestController
@RequestMapping("/api/auth")
/**
 * 鉴权接口：注册、登录、个人信息查询、修改密码，找回密码为占位实现。
 */
public class AuthController {

    @Resource
    private ISysUserService sysUserService;
    @Resource
    private ActivityService activityService;
    @Resource
    private RateLimitService rateLimitService;

    /** 邮件通道开关，未接入时重置令牌随响应返回便于本地联调 */
    @Value("${app.mail.enabled:false}")
    private boolean mailEnabled;

    @AuthAccess
    @PostMapping("/register")
    public Result register(@Valid @RequestBody RegisterRequest form, HttpServletRequest request) {
        rateLimitService.checkIp("register", clientIp(request));
        sysUserService.register(form);
        return Result.success();
    }

    @AuthAccess
    @PostMapping("/login")
    public Result login(@Valid @RequestBody LoginRequest form, HttpServletRequest request) {
        rateLimitService.checkIp("login", clientIp(request));
        SysUser user = sysUserService.login(form.getUsername(), form.getPassword());
        activityService.record(user.getId(), "login");
        String token = TokenUtils.createToken(user.getId(), user.getRole(), user.getPassword());
        user.setPassword(null);
        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        data.put("user", user);
        return Result.success(data);
    }

    @GetMapping("/profile")
    public Result profile() {
        SysUser user = TokenUtils.getCurrentUser();
        if (user == null) {
            throw new ServiceException(Constants.CODE_401, "未登录");
        }
        user.setPassword(null);
        return Result.success(user);
    }

    @PutMapping("/password")
    public Result changePassword(@Valid @RequestBody ChangePasswordRequest form) {
        SysUser user = TokenUtils.getCurrentUser();
        if (user == null) {
            throw new ServiceException(Constants.CODE_401, "未登录");
        }
        sysUserService.changePassword(user.getId(), form.getPassword(), form.getNewPassword());
        return Result.success();
    }

    @AuthAccess
    @PostMapping("/forgot")
    public Result forgot(@Valid @RequestBody ForgotPasswordRequest form, HttpServletRequest request) {
        rateLimitService.checkIp("forgot", clientIp(request));
        String token = sysUserService.forgotPassword(form.getEmail().trim());
        Map<String, Object> data = new HashMap<>();
        data.put("sent", token != null);
        data.put("expireMinutes", 30);
        // 邮件通道未接入时把令牌随响应返回，接入后置 app.mail.enabled 为 true 即不再返回
        if (token != null && !mailEnabled) {
            data.put("token", token);
        }
        return Result.success(data);
    }

    @AuthAccess
    @PostMapping("/reset")
    public Result reset(@Valid @RequestBody ResetPasswordRequest form, HttpServletRequest request) {
        rateLimitService.checkIp("reset", clientIp(request));
        sysUserService.resetPassword(form.getToken().trim(), form.getNewPassword());
        return Result.success();
    }

    /**
     * 取请求来源 IP，反向代理场景优先取 X-Forwarded-For 首个地址
     */
    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            int comma = forwarded.indexOf(',');
            return (comma > 0 ? forwarded.substring(0, comma) : forwarded).trim();
        }
        return request.getRemoteAddr();
    }
}
