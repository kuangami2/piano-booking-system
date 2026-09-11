package com.roomreservation.controller;

import com.roomreservation.common.Result;
import com.roomreservation.entity.SysUser;
import com.roomreservation.service.ActivityService;
import com.roomreservation.service.RateLimitService;
import com.roomreservation.utils.TokenUtils;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 活跃度排行榜，契约 v0.5 冻结
 */
@RestController
@RequestMapping("/api/rankings")
/**
 * 排行榜接口：按周榜或月榜查询活跃度排名，按用户每分钟 30 次限流。
 */
public class RankingController {

    @Resource
    private ActivityService activityService;
    @Resource
    private RateLimitService rateLimitService;

    @GetMapping("/activity")
    public Result activity(@RequestParam(defaultValue = "week") String period,
                           @RequestParam(defaultValue = "1") Integer page,
                           @RequestParam(defaultValue = "20") Integer size) {
        SysUser user = TokenUtils.getCurrentUser();
        rateLimitService.check("ranking", user.getId(), 30);
        return Result.success(activityService.ranking(period, page, size, user.getId()));
    }
}
