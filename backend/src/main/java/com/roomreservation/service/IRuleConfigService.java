package com.roomreservation.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.roomreservation.entity.RuleConfig;

/**
 * 规则参数 Service，读内存缓存，修改后 refreshCache 即时生效
 */
public interface IRuleConfigService extends IService<RuleConfig> {

    /**
     * 读取规则整数值，不存在返回默认值
     */
    int getInt(String key, int defaultValue);

    /**
     * 全量刷新规则缓存，管理端改参后调用
     */
    void refreshCache();
}
