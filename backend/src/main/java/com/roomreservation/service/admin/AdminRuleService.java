package com.roomreservation.service.admin;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.roomreservation.common.Constants;
import com.roomreservation.entity.RuleConfig;
import com.roomreservation.exception.ServiceException;
import com.roomreservation.service.IRuleConfigService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 管理端规则 Service：全量读取与批量更新，改后刷新缓存即时生效
 */
@Service
public class AdminRuleService {

    @Resource
    private IRuleConfigService ruleConfigService;

    /**
     * 全量规则参数，按 id 排序
     */
    public List<RuleConfig> listAll() {
        return ruleConfigService.list(new LambdaQueryWrapper<RuleConfig>().orderByAsc(RuleConfig::getId));
    }

    /**
     * 批量更新规则值，请求体 [{key,value}]，全部成功并刷新缓存
     */
    public List<RuleConfig> batchUpdate(List<Map<String, Object>> rules) {
        if (rules == null || rules.isEmpty()) {
            throw new ServiceException(Constants.CODE_400, "更新列表不能为空");
        }
        List<String> updatedKeys = new ArrayList<>();
        for (Map<String, Object> item : rules) {
            String key = item.get("key") == null ? null : item.get("key").toString().trim();
            Object valueObj = item.get("value");
            if (StrUtil.isBlank(key)) {
                throw new ServiceException(Constants.CODE_400, "规则 key 不能为空");
            }
            Long exists = ruleConfigService.count(new LambdaQueryWrapper<RuleConfig>()
                    .eq(RuleConfig::getRuleKey, key));
            if (exists == null || exists == 0) {
                throw new ServiceException(Constants.CODE_400, "规则 " + key + " 不存在");
            }
            if (valueObj == null || StrUtil.isBlank(valueObj.toString())) {
                throw new ServiceException(Constants.CODE_400, "规则 " + key + " 的值不能为空");
            }
            ruleConfigService.update(new LambdaUpdateWrapper<RuleConfig>()
                    .eq(RuleConfig::getRuleKey, key)
                    .set(RuleConfig::getRuleValue, valueObj.toString().trim()));
            updatedKeys.add(key);
        }
        // 改后刷新内存缓存，预约校验即时生效
        ruleConfigService.refreshCache();
        return listAll();
    }
}
