package com.roomreservation.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.roomreservation.common.Result;
import com.roomreservation.entity.Banner;
import com.roomreservation.entity.Notice;
import com.roomreservation.entity.RuleConfig;
import com.roomreservation.mapper.BannerMapper;
import com.roomreservation.mapper.NoticeMapper;
import com.roomreservation.mapper.RuleConfigMapper;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 首页公共内容与规则公开视图：轮播、公告、可预约参数
 */
@RestController
@RequestMapping("/api")
public class ContentController {

    @Resource
    private BannerMapper bannerMapper;
    @Resource
    private NoticeMapper noticeMapper;
    @Resource
    private RuleConfigMapper ruleConfigMapper;

    @GetMapping("/banners")
    public Result banners() {
        List<Banner> list = bannerMapper.selectList(new LambdaQueryWrapper<Banner>()
                .eq(Banner::getStatus, "normal")
                .orderByAsc(Banner::getSort));
        return Result.success(list);
    }

    @GetMapping("/notices")
    public Result notices() {
        List<Notice> list = noticeMapper.selectList(new LambdaQueryWrapper<Notice>()
                .eq(Notice::getStatus, "normal")
                .orderByDesc(Notice::getId));
        return Result.success(list);
    }

    /**
     * 规则公开视图，供前端预约表单约束与提示
     */
    @GetMapping("/rules")
    public Result rules() {
        Map<String, Object> data = new LinkedHashMap<>();
        List<RuleConfig> all = ruleConfigMapper.selectList(null);
        for (RuleConfig cfg : all) {
            data.put(cfg.getRuleKey(), cfg.getRuleValue());
        }
        return Result.success(data);
    }
}
