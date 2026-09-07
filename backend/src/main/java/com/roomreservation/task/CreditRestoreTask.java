package com.roomreservation.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.roomreservation.common.RuleKeys;
import com.roomreservation.entity.CreditLog;
import com.roomreservation.entity.SysUser;
import com.roomreservation.mapper.CreditLogMapper;
import com.roomreservation.mapper.SysUserMapper;
import com.roomreservation.service.IRuleConfigService;
import jakarta.annotation.Resource;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 信用分每日自然恢复：低于上限的用户按规则每日加回，记入信用流水
 */
@Component
public class CreditRestoreTask {

    @Resource
    private SysUserMapper sysUserMapper;
    @Resource
    private CreditLogMapper creditLogMapper;
    @Resource
    private IRuleConfigService ruleConfigService;

    @Scheduled(cron = "0 10 0 * * ?")
    public void dailyRestore() {
        int max = ruleConfigService.getInt(RuleKeys.CREDIT_MAX, 100);
        int daily = ruleConfigService.getInt(RuleKeys.CREDIT_DAILY_RESTORE, 1);
        if (daily <= 0) {
            return;
        }
        List<SysUser> users = sysUserMapper.selectList(
                new LambdaQueryWrapper<SysUser>().lt(SysUser::getCredit, max));
        for (SysUser user : users) {
            int credit = user.getCredit() == null ? 0 : user.getCredit();
            int add = Math.min(daily, max - credit);
            if (add <= 0) {
                continue;
            }
            sysUserMapper.update(null, new LambdaUpdateWrapper<SysUser>()
                    .eq(SysUser::getId, user.getId())
                    .set(SysUser::getCredit, credit + add));
            CreditLog log = new CreditLog();
            log.setUserId(user.getId());
            log.setChangeValue(add);
            log.setReason("信用自然恢复");
            creditLogMapper.insert(log);
        }
    }
}
