package com.roomreservation.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.roomreservation.common.RuleCatalog;
import com.roomreservation.entity.RuleConfig;
import com.roomreservation.mapper.RuleConfigMapper;
import com.roomreservation.service.IRuleConfigService;
import jakarta.annotation.PostConstruct;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * 规则参数 Service 实现，内存缓存，启动与定时全量加载，管理端改参后主动刷新
 */
@Service
/**
 * 规则参数实现：启动加载进内存缓存，管理端改参后主动刷新，另有定时兜底刷新。
 */
public class RuleConfigServiceImpl extends ServiceImpl<RuleConfigMapper, RuleConfig> implements IRuleConfigService {

    private volatile Map<String, String> cache = new HashMap<>();

    @PostConstruct
    public void init() {
        refreshCache();
    }

    @Override
    public synchronized void refreshCache() {
        Map<String, String> map = new HashMap<>();
        for (RuleConfig cfg : list()) {
            if (cfg.getRuleKey() != null) {
                map.put(cfg.getRuleKey(), cfg.getRuleValue());
            }
        }
        cache = map;
    }

    @Override
    public int getInt(String key, int defaultValue) {
        String value = cache.get(key);
        if (value == null) {
            return RuleCatalog.clamp(key, defaultValue);
        }
        try {
            // 统一钳制到合法区间，脏数据或绕过校验的写入不会导致除零等崩溃
            return RuleCatalog.clamp(key, Integer.parseInt(value.trim()));
        } catch (NumberFormatException e) {
            return RuleCatalog.clamp(key, defaultValue);
        }
    }

    /**
     * 兜底周期刷新，防止外部改库后长期不一致
     */
    @Scheduled(fixedDelay = 300000)
    public void scheduledRefresh() {
        refreshCache();
    }
}
