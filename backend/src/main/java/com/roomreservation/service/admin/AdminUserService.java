package com.roomreservation.service.admin;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.BCrypt;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.roomreservation.common.Constants;
import com.roomreservation.entity.SysUser;
import com.roomreservation.exception.ServiceException;
import com.roomreservation.mapper.SysUserMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * 管理端用户 Service：用户列表、设置会员、重置密码
 */
@Service
public class AdminUserService {

    @Resource
    private SysUserMapper sysUserMapper;

    /**
     * 用户分页列表，keyword 匹配登录名、姓名或学号
     */
    public Map<String, Object> pageUsers(String keyword, Integer page, Integer size) {
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        if (StrUtil.isNotBlank(keyword)) {
            wrapper.and(w -> w.like(SysUser::getUsername, keyword)
                    .or().like(SysUser::getName, keyword)
                    .or().like(SysUser::getStudentNo, keyword));
        }
        wrapper.orderByAsc(SysUser::getId);
        Page<SysUser> result = sysUserMapper.selectPage(new Page<>(page, size), wrapper);
        result.getRecords().forEach(user -> user.setPassword(null));
        Map<String, Object> data = new HashMap<>();
        data.put("list", result.getRecords());
        data.put("total", result.getTotal());
        return data;
    }

    /**
     * 设置会员标记
     */
    public void setMember(Integer id, Boolean isMember) {
        if (isMember == null) {
            throw new ServiceException(Constants.CODE_400, "isMember 不能为空");
        }
        requireUser(id);
        sysUserMapper.update(null, new LambdaUpdateWrapper<SysUser>()
                .eq(SysUser::getId, id)
                .set(SysUser::getIsMember, isMember));
    }

    /**
     * 重置密码为指定新密码
     */
    public void resetPassword(Integer id, String newPassword) {
        if (StrUtil.isBlank(newPassword) || newPassword.length() < 6) {
            throw new ServiceException(Constants.CODE_400, "新密码至少 6 位");
        }
        requireUser(id);
        sysUserMapper.update(null, new LambdaUpdateWrapper<SysUser>()
                .eq(SysUser::getId, id)
                .set(SysUser::getPassword, BCrypt.hashpw(newPassword)));
    }

    private void requireUser(Integer id) {
        if (id == null || sysUserMapper.selectById(id) == null) {
            throw new ServiceException(Constants.CODE_404, "用户不存在");
        }
    }
}
