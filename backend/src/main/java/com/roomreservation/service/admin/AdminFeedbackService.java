package com.roomreservation.service.admin;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.roomreservation.common.Constants;
import com.roomreservation.entity.Feedback;
import com.roomreservation.entity.SysUser;
import com.roomreservation.exception.ServiceException;
import com.roomreservation.mapper.FeedbackMapper;
import com.roomreservation.mapper.SysUserMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 管理端反馈 Service：反馈列表与状态处理
 */
@Service
public class AdminFeedbackService {

    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final List<String> STATUSES = List.of("pending", "processed", "ignored");

    @Resource
    private FeedbackMapper feedbackMapper;
    @Resource
    private SysUserMapper sysUserMapper;

    /**
     * 反馈分页列表，可按状态过滤，附用户信息
     */
    public Map<String, Object> page(String status, Integer page, Integer size) {
        if (StrUtil.isNotBlank(status) && !STATUSES.contains(status)) {
            throw new ServiceException(Constants.CODE_400, "status 只能为 pending、processed 或 ignored");
        }
        LambdaQueryWrapper<Feedback> wrapper = new LambdaQueryWrapper<>();
        if (StrUtil.isNotBlank(status)) {
            wrapper.eq(Feedback::getStatus, status);
        }
        wrapper.orderByDesc(Feedback::getId);
        Page<Feedback> result = feedbackMapper.selectPage(new Page<>(page, size), wrapper);
        List<Feedback> records = result.getRecords();
        Set<Integer> ids = new HashSet<>();
        for (Feedback f : records) {
            ids.add(f.getUserId());
        }
        Map<Integer, SysUser> users = new HashMap<>();
        if (!ids.isEmpty()) {
            for (SysUser u : sysUserMapper.selectBatchIds(ids)) {
                users.put(u.getId(), u);
            }
        }
        List<Map<String, Object>> rows = new ArrayList<>();
        for (Feedback f : records) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", f.getId());
            row.put("userId", f.getUserId());
            row.put("content", f.getContent());
            row.put("status", f.getStatus());
            row.put("createdAt", f.getCreatedAt() == null ? null : f.getCreatedAt().format(DATE_TIME));
            SysUser user = users.get(f.getUserId());
            row.put("username", user == null ? null : user.getUsername());
            row.put("userName", user == null ? null : user.getName());
            rows.add(row);
        }
        Map<String, Object> data = new HashMap<>();
        data.put("list", rows);
        data.put("total", result.getTotal());
        return data;
    }

    /**
     * 处理反馈，状态取值 pending、processed、ignored
     */
    public void handleStatus(Integer id, String status) {
        if (id == null || feedbackMapper.selectById(id) == null) {
            throw new ServiceException(Constants.CODE_404, "反馈不存在");
        }
        if (StrUtil.isBlank(status) || !STATUSES.contains(status)) {
            throw new ServiceException(Constants.CODE_400, "status 只能为 pending、processed 或 ignored");
        }
        feedbackMapper.update(null, new LambdaUpdateWrapper<Feedback>()
                .eq(Feedback::getId, id)
                .set(Feedback::getStatus, status));
    }
}
