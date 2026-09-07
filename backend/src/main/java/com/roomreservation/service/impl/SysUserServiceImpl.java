package com.roomreservation.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.BCrypt;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.roomreservation.common.Constants;
import com.roomreservation.entity.SysUser;
import com.roomreservation.exception.ServiceException;
import com.roomreservation.mapper.SysUserMapper;
import com.roomreservation.service.ISysUserService;
import org.springframework.stereotype.Service;

/**
 * 用户 Service 实现
 */
@Service
public class SysUserServiceImpl extends ServiceImpl<SysUserMapper, SysUser> implements ISysUserService {

    @Override
    public void register(SysUser user) {
        String username = user.getUsername();
        String password = user.getPassword();
        String name = user.getName();
        String studentNo = user.getStudentNo();
        String email = user.getEmail();
        if (StrUtil.hasBlank(username, password, name, studentNo, email)) {
            throw new ServiceException(Constants.CODE_400, "姓名、学号、邮箱、账号与密码均不能为空");
        }
        if (password.length() < 6) {
            throw new ServiceException(Constants.CODE_400, "密码至少 6 位");
        }
        if (count(new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, username)) > 0) {
            throw new ServiceException(Constants.CODE_400, "该登录名已被注册");
        }
        if (count(new LambdaQueryWrapper<SysUser>().eq(SysUser::getStudentNo, studentNo)) > 0) {
            throw new ServiceException(Constants.CODE_400, "该学号已注册");
        }
        if (count(new LambdaQueryWrapper<SysUser>().eq(SysUser::getEmail, email)) > 0) {
            throw new ServiceException(Constants.CODE_400, "该邮箱已注册");
        }
        SysUser target = new SysUser();
        target.setUsername(username);
        target.setPassword(BCrypt.hashpw(password));
        target.setName(name);
        target.setStudentNo(studentNo);
        target.setEmail(email);
        target.setRole("user");
        target.setIsMember(false);
        target.setCredit(100);
        target.setStatus("normal");
        save(target);
    }

    @Override
    public SysUser login(String username, String password) {
        if (StrUtil.hasBlank(username, password)) {
            throw new ServiceException(Constants.CODE_400, "账号与密码不能为空");
        }
        SysUser user = getOne(new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, username));
        if (user == null) {
            throw new ServiceException(Constants.CODE_400, "账号不存在");
        }
        if (!BCrypt.checkpw(password, user.getPassword())) {
            throw new ServiceException(Constants.CODE_400, "密码错误");
        }
        return user;
    }

    @Override
    public void changePassword(Integer userId, String oldPassword, String newPassword) {
        if (StrUtil.hasBlank(oldPassword, newPassword)) {
            throw new ServiceException(Constants.CODE_400, "旧密码与新密码不能为空");
        }
        if (newPassword.length() < 6) {
            throw new ServiceException(Constants.CODE_400, "新密码至少 6 位");
        }
        SysUser user = getById(userId);
        if (user == null) {
            throw new ServiceException(Constants.CODE_401, "用户不存在，请重新登录");
        }
        if (!BCrypt.checkpw(oldPassword, user.getPassword())) {
            throw new ServiceException(Constants.CODE_400, "旧密码错误");
        }
        SysUser target = new SysUser();
        target.setId(userId);
        target.setPassword(BCrypt.hashpw(newPassword));
        updateById(target);
    }
}
