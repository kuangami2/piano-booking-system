package com.roomreservation.service.admin;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.roomreservation.common.Constants;
import com.roomreservation.common.RuleKeys;
import com.roomreservation.entity.CreditLog;
import com.roomreservation.entity.SysUser;
import com.roomreservation.exception.ServiceException;
import com.roomreservation.mapper.CreditLogMapper;
import com.roomreservation.mapper.SysUserMapper;
import com.roomreservation.service.IRuleConfigService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
 * 管理端信用 Service：信用流水、人工减分与恢复；低于阈值后预约创建自然被暂停
 */
@Service
public class AdminCreditService {

    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Resource
    private SysUserMapper sysUserMapper;
    @Resource
    private CreditLogMapper creditLogMapper;
    @Resource
    private IRuleConfigService ruleConfigService;

    /**
     * 信用流水分页，可按用户过滤，附用户与经办管理员姓名
     */
    public Map<String, Object> pageLogs(Integer userId, Integer page, Integer size) {
        LambdaQueryWrapper<CreditLog> wrapper = new LambdaQueryWrapper<>();
        if (userId != null) {
            wrapper.eq(CreditLog::getUserId, userId);
        }
        wrapper.orderByDesc(CreditLog::getId);
        Page<CreditLog> result = creditLogMapper.selectPage(new Page<>(page, size), wrapper);
        List<CreditLog> records = result.getRecords();
        Set<Integer> ids = new HashSet<>();
        for (CreditLog log : records) {
            if (log.getUserId() != null) {
                ids.add(log.getUserId());
            }
            if (log.getOperatorId() != null) {
                ids.add(log.getOperatorId());
            }
        }
        Map<Integer, SysUser> users = new HashMap<>();
        if (!ids.isEmpty()) {
            for (SysUser u : sysUserMapper.selectBatchIds(ids)) {
                users.put(u.getId(), u);
            }
        }
        List<Map<String, Object>> rows = new ArrayList<>();
        for (CreditLog log : records) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", log.getId());
            row.put("userId", log.getUserId());
            row.put("changeValue", log.getChangeValue());
            row.put("reason", log.getReason());
            row.put("operatorId", log.getOperatorId());
            row.put("createdAt", log.getCreatedAt() == null ? null : log.getCreatedAt().format(DATE_TIME));
            SysUser user = users.get(log.getUserId());
            row.put("username", user == null ? null : user.getUsername());
            row.put("userName", user == null ? null : user.getName());
            SysUser operator = users.get(log.getOperatorId());
            row.put("operatorName", operator == null ? null : operator.getName());
            rows.add(row);
        }
        Map<String, Object> data = new HashMap<>();
        data.put("list", rows);
        data.put("total", result.getTotal());
        return data;
    }

    /**
     * 人工减分，信用不低于 0；低于暂停阈值时预约自动暂停
     */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> deduct(Integer operatorId, Integer userId, Integer points, String reason) {
        check(userId, points, reason);
        SysUser user = requireUser(userId);
        int lowThreshold = ruleConfigService.getInt(RuleKeys.CREDIT_LOW, 60);
        int oldCredit = user.getCredit() == null ? 0 : user.getCredit();
        int newCredit = Math.max(0, oldCredit - points);
        updateCredit(userId, newCredit);
        insertLog(userId, -points, reason, operatorId);
        Map<String, Object> data = new HashMap<>();
        data.put("userId", userId);
        data.put("credit", newCredit);
        data.put("paused", newCredit < lowThreshold);
        return data;
    }

    /**
     * 人工恢复加分，不超过信用上限规则
     */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> restore(Integer operatorId, Integer userId, Integer points, String reason) {
        check(userId, points, reason);
        SysUser user = requireUser(userId);
        int maxCredit = ruleConfigService.getInt(RuleKeys.CREDIT_MAX, 100);
        int oldCredit = user.getCredit() == null ? 0 : user.getCredit();
        int newCredit = Math.min(maxCredit, oldCredit + points);
        updateCredit(userId, newCredit);
        insertLog(userId, points, reason, operatorId);
        Map<String, Object> data = new HashMap<>();
        data.put("userId", userId);
        data.put("credit", newCredit);
        return data;
    }

    private void check(Integer userId, Integer points, String reason) {
        if (points == null || points <= 0) {
            throw new ServiceException(Constants.CODE_400, "分数必须为正整数");
        }
        if (StrUtil.isBlank(reason)) {
            throw new ServiceException(Constants.CODE_400, "操作原因不能为空");
        }
        if (userId == null) {
            throw new ServiceException(Constants.CODE_400, "userId 不能为空");
        }
    }

    private SysUser requireUser(Integer userId) {
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            throw new ServiceException(Constants.CODE_404, "用户不存在");
        }
        return user;
    }

    private void updateCredit(Integer userId, int credit) {
        sysUserMapper.update(null, new LambdaUpdateWrapper<SysUser>()
                .eq(SysUser::getId, userId)
                .set(SysUser::getCredit, credit));
    }

    private void insertLog(Integer userId, int changeValue, String reason, Integer operatorId) {
        CreditLog log = new CreditLog();
        log.setUserId(userId);
        log.setChangeValue(changeValue);
        log.setReason(reason);
        log.setOperatorId(operatorId);
        creditLogMapper.insert(log);
    }
}
