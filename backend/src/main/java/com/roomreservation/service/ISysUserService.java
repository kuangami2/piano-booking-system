package com.roomreservation.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.roomreservation.dto.RegisterRequest;
import com.roomreservation.entity.SysUser;

/**
 * 用户 Service
 */
public interface ISysUserService extends IService<SysUser> {

    /**
     * 注册，字段格式由请求对象校验，此处负责唯一性与密码加密
     */
    void register(RegisterRequest form);

    /**
     * 登录，校验密码，返回脱敏后用户
     */
    SysUser login(String username, String password);

    /**
     * 修改密码，校验旧密码
     */
    void changePassword(Integer userId, String oldPassword, String newPassword);

    /**
     * 找回密码，生成一次性重置令牌，邮箱不存在时返回 null 不暴露注册状态
     */
    String forgotPassword(String email);

    /**
     * 凭令牌重置密码，令牌一次性且过期失效，重置后其余令牌一并作废
     */
    void resetPassword(String token, String newPassword);

}
