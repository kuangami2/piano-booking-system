package com.roomreservation.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.roomreservation.entity.RuleConfig;
import com.roomreservation.mapper.RuleConfigMapper;
import com.roomreservation.service.IRuleConfigService;
import org.springframework.stereotype.Service;

/**
 * 规则参数 Service 实现，先读库，缓存留后续按修改刷新
 */
@Service
public class RuleConfigServiceImpl extends ServiceImpl<RuleConfigMapper, RuleConfig> implements IRuleConfigService {

    @Override
    public int getInt(String key, int defaultValue) {
        RuleConfig cfg = getOne(new LambdaQueryWrapper<RuleConfig>().eq(RuleConfig::getRuleKey, key));
        if (cfg == null || cfg.getRuleValue() == null) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(cfg.getRuleValue().trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}
