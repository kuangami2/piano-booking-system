package com.roomreservation.controller;

import cn.hutool.core.util.StrUtil;
import com.roomreservation.common.Constants;
import com.roomreservation.common.Result;
import com.roomreservation.config.interceptor.AuthAccess;
import com.roomreservation.entity.SysUser;
import com.roomreservation.exception.ServiceException;
import com.roomreservation.service.ActivityService;
import com.roomreservation.service.ISysUserService;
import com.roomreservation.utils.TokenUtils;
import jakarta.annotation.Resource;
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

    @AuthAccess
    @PostMapping("/register")
    public Result register(@RequestBody SysUser user) {
        sysUserService.register(user);
        return Result.success();
    }

    @AuthAccess
    @PostMapping("/login")
    public Result login(@RequestBody SysUser loginUser) {
        SysUser user = sysUserService.login(loginUser.getUsername(), loginUser.getPassword());
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
    public Result changePassword(@RequestBody SysUser form) {
        SysUser user = TokenUtils.getCurrentUser();
        if (user == null) {
            throw new ServiceException(Constants.CODE_401, "未登录");
        }
        sysUserService.changePassword(user.getId(), form.getPassword(), form.getNewPassword());
        return Result.success();
    }

    @AuthAccess
    @PostMapping("/forgot")
    public Result forgot(@RequestBody SysUser form) {
        if (StrUtil.isBlank(form.getEmail())) {
            throw new ServiceException(Constants.CODE_400, "邮箱不能为空");
        }
        // 邮箱发送通道待接入，先提示管理员重置
        return Result.error(Constants.CODE_400, "邮件发送通道未接入，请联系管理员重置密码");
    }

    @AuthAccess
    @PostMapping("/reset")
    public Result reset(@RequestBody SysUser form) {
        // 重置链接 token 校验待接入
        return Result.error(Constants.CODE_400, "重置链接校验通道未接入，请联系管理员重置密码");
    }
}
