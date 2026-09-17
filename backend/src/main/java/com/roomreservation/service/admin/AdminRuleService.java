package com.roomreservation.service.admin;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.roomreservation.common.Constants;
import com.roomreservation.common.RuleCatalog;
import com.roomreservation.common.RuleKeys;
import com.roomreservation.entity.RuleConfig;
import com.roomreservation.exception.ServiceException;
import com.roomreservation.service.CacheService;
import com.roomreservation.service.IRuleConfigService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 管理端规则 Service：全量读取与批量更新，改后刷新缓存即时生效
 */
@Service
/**
 * 管理端规则业务：参数读取与批量保存，改后刷新参数缓存并失效受影响的派生缓存。
 */
public class AdminRuleService {

    /**
     * 空闲查询按最小单位铺块、按提前天数裁剪日期，改这两项必须让 free 缓存立刻失效，
     * 否则缓存存活期内展示口径与提交校验口径不一致。
     */
    private static final Set<String> FREE_SLOT_AFFECTING = Set.of(
            RuleKeys.BOOKING_MIN_UNIT, RuleKeys.BOOKING_ADVANCE_DAYS);

    @Resource
    private IRuleConfigService ruleConfigService;
    @Resource
    private CacheService cacheService;

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
        // 第一阶段：全部校验通过后再落库，避免部分写入
        Map<String, Integer> effective = currentValues();
        Map<String, String> pending = new LinkedHashMap<>();
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
            int value;
            try {
                value = Integer.parseInt(valueObj.toString().trim());
            } catch (NumberFormatException e) {
                throw new ServiceException(Constants.CODE_400, "规则 " + key + " 的值应为整数");
            }
            String rangeError = RuleCatalog.validateRange(key, value);
            if (rangeError != null) {
                throw new ServiceException(Constants.CODE_400, rangeError);
            }
            pending.put(key, String.valueOf(value));
            effective.put(key, value);
        }
        String dependencyError = RuleCatalog.validateDependencies(effective);
        if (dependencyError != null) {
            throw new ServiceException(Constants.CODE_400, dependencyError);
        }
        // 第二阶段：写入并刷新内存缓存，预约校验即时生效
        for (Map.Entry<String, String> entry : pending.entrySet()) {
            ruleConfigService.update(new LambdaUpdateWrapper<RuleConfig>()
                    .eq(RuleConfig::getRuleKey, entry.getKey())
                    .set(RuleConfig::getRuleValue, entry.getValue()));
        }
        ruleConfigService.refreshCache();
        // 派生缓存失效：空闲区间按旧粒度或旧提前天数算出的结果不能再展示
        if (pending.keySet().stream().anyMatch(FREE_SLOT_AFFECTING::contains)) {
            cacheService.evictByPrefix("free:");
        }
        return listAll();
    }

    /**
     * 库中当前生效值，依赖校验时补齐本次未提交的参数。
     * 经 RuleCatalog.clamp 归一，与读取侧生效值保持一致，脏数据不会让校验视图失真或抛异常。
     */
    private Map<String, Integer> currentValues() {
        Map<String, Integer> values = new HashMap<>();
        for (RuleConfig config : listAll()) {
            try {
                values.put(config.getRuleKey(),
                        RuleCatalog.clamp(config.getRuleKey(), Integer.parseInt(config.getRuleValue().trim())));
            } catch (RuntimeException ignored) {
                // 非整数值跳过，读取侧会钳制到合法区间
            }
        }
        return values;
    }
}
