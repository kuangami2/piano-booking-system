package com.roomreservation.service.impl;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.BCrypt;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.roomreservation.common.Constants;
import com.roomreservation.dto.RegisterRequest;
import com.roomreservation.entity.PasswordReset;
import com.roomreservation.entity.SysUser;
import com.roomreservation.exception.ServiceException;
import com.roomreservation.mapper.PasswordResetMapper;
import com.roomreservation.mapper.SysUserMapper;
import com.roomreservation.service.ISysUserService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 用户 Service 实现
 */
@Service
@Slf4j
/**
 * 用户业务实现：唯一性校验、BCrypt 密码处理、改密与令牌找回。
 */
public class SysUserServiceImpl extends ServiceImpl<SysUserMapper, SysUser> implements ISysUserService {

    /** 重置令牌有效分钟数 */
    private static final int RESET_TOKEN_MINUTES = 30;

    @Resource
    private PasswordResetMapper passwordResetMapper;

    @Override
    public void register(RegisterRequest form) {
        String username = form.getUsername().trim();
        String studentNo = form.getStudentNo().trim();
        String email = form.getEmail().trim();
        // 先查重给出明确提示，并发场景由唯一索引兜底
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
        target.setPassword(BCrypt.hashpw(form.getPassword()));
        target.setName(form.getName().trim());
        target.setStudentNo(studentNo);
        target.setEmail(email);
        target.setRole("user");
        target.setIsMember(false);
        target.setCredit(100);
        target.setStatus("normal");
        try {
            save(target);
        } catch (DuplicateKeyException e) {
            throw new ServiceException(Constants.CODE_400, duplicateMessage(e));
        }
    }

    /**
     * 唯一索引冲突按命中的索引转为字段级提示
     */
    private String duplicateMessage(DuplicateKeyException e) {
        String message = e.getMessage() == null ? "" : e.getMessage();
        if (message.contains("uk_user_student_no")) {
            return "该学号已注册";
        }
        if (message.contains("uk_user_email")) {
            return "该邮箱已注册";
        }
        if (message.contains("username")) {
            return "该登录名已被注册";
        }
        return "账号信息重复，请检查登录名、学号与邮箱";
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
        // 新密码格式由请求对象校验，此处只核对账号与旧密码
        SysUser user = getById(userId);
        if (user == null) {
            throw new ServiceException(Constants.CODE_401, "用户不存在，请重新登录");
        }
        if (!BCrypt.checkpw(oldPassword, user.getPassword())) {
            throw new ServiceException(Constants.CODE_400, "旧密码错误");
        }
        update(new LambdaUpdateWrapper<SysUser>()
                .eq(SysUser::getId, userId)
                .set(SysUser::getPassword, BCrypt.hashpw(newPassword)));
    }

    @Override
    public String forgotPassword(String email) {
        SysUser user = getOne(new LambdaQueryWrapper<SysUser>().eq(SysUser::getEmail, email));
        if (user == null) {
            // 邮箱未注册时不暴露状态，接口统一按已发送提示
            return null;
        }
        // 作废该用户此前未使用的令牌，保证同一时间只有一个有效令牌
        passwordResetMapper.update(null, new LambdaUpdateWrapper<PasswordReset>()
                .eq(PasswordReset::getUserId, user.getId())
                .eq(PasswordReset::getUsed, false)
                .set(PasswordReset::getUsed, true));
        PasswordReset reset = new PasswordReset();
        reset.setUserId(user.getId());
        reset.setToken(IdUtil.fastSimpleUUID());
        reset.setExpireAt(LocalDateTime.now().plusMinutes(RESET_TOKEN_MINUTES));
        reset.setUsed(false);
        passwordResetMapper.insert(reset);
        log.info("生成密码重置令牌 userId={} token={} 有效期 {} 分钟", user.getId(), reset.getToken(), RESET_TOKEN_MINUTES);
        return reset.getToken();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void resetPassword(String token, String newPassword) {
        PasswordReset reset = passwordResetMapper.selectOne(new LambdaQueryWrapper<PasswordReset>()
                .eq(PasswordReset::getToken, token));
        if (reset == null || Boolean.TRUE.equals(reset.getUsed())) {
            throw new ServiceException(Constants.CODE_400, "重置令牌无效或已使用");
        }
        if (reset.getExpireAt() == null || reset.getExpireAt().isBefore(LocalDateTime.now())) {
            throw new ServiceException(Constants.CODE_400, "重置令牌已过期，请重新申请");
        }
        update(new LambdaUpdateWrapper<SysUser>()
                .eq(SysUser::getId, reset.getUserId())
                .set(SysUser::getPassword, BCrypt.hashpw(newPassword)));
        // 重置成功即作废该用户全部令牌
        passwordResetMapper.update(null, new LambdaUpdateWrapper<PasswordReset>()
                .eq(PasswordReset::getUserId, reset.getUserId())
                .set(PasswordReset::getUsed, true));
        log.info("密码重置成功 userId={}", reset.getUserId());
    }
}
