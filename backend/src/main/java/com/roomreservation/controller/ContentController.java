package com.roomreservation.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.roomreservation.common.Result;
import com.roomreservation.common.RuleCatalog;
import com.roomreservation.entity.Banner;
import com.roomreservation.entity.Notice;
import com.roomreservation.mapper.BannerMapper;
import com.roomreservation.mapper.NoticeMapper;
import com.roomreservation.service.IRuleConfigService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 首页公共内容与规则公开视图：轮播、公告、可预约参数
 */
@RestController
@RequestMapping("/api")
/**
 * 内容接口：轮播、公告与规则参数公开视图，供首页与预约表单使用。
 */
public class ContentController {

    @Resource
    private BannerMapper bannerMapper;
    @Resource
    private NoticeMapper noticeMapper;
    @Resource
    private IRuleConfigService ruleConfigService;

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
     * 规则公开视图，供前端预约表单约束与提示。
     * 取值走 RuleConfigService 的读取钳制，脏数据或绕过校验的写入不会透出给前端。
     */
    @GetMapping("/rules")
    public Result rules() {
        Map<String, Object> data = new LinkedHashMap<>();
        for (RuleCatalog.Meta meta : RuleCatalog.all()) {
            data.put(meta.key(), String.valueOf(ruleConfigService.getInt(meta.key(), meta.defaultValue())));
        }
        return Result.success(data);
    }
}
