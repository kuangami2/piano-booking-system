package com.roomreservation.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.roomreservation.entity.SysUser;

/**
 * 用户 Service
 */
public interface ISysUserService extends IService<SysUser> {

    /**
     * 注册，校验唯一性，密码 BCrypt 加密
     */
    void register(SysUser user);

    /**
     * 登录，校验密码，返回脱敏后用户
     */
    SysUser login(String username, String password);

    /**
     * 修改密码，校验旧密码
     */
    void changePassword(Integer userId, String oldPassword, String newPassword);

}
