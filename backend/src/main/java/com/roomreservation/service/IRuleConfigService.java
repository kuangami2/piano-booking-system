package com.roomreservation.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.roomreservation.entity.RuleConfig;

/**
 * 规则参数 Service
 */
public interface IRuleConfigService extends IService<RuleConfig> {

    /**
     * 读取规则整数值，不存在返回默认值
     */
    int getInt(String key, int defaultValue);
}
