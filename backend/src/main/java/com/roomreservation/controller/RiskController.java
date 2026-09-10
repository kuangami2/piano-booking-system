package com.roomreservation.controller;

import com.roomreservation.common.Result;
import com.roomreservation.entity.SysUser;
import com.roomreservation.service.RateLimitService;
import com.roomreservation.service.RiskService;
import com.roomreservation.utils.TokenUtils;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 临近取消风控状态，仅返回本人信息，契约 v0.5 冻结
 */
@RestController
@RequestMapping("/api/risk")
public class RiskController {

    @Resource
    private RiskService riskService;
    @Resource
    private RateLimitService rateLimitService;

    @GetMapping("/near-cancel")
    public Result nearCancel() {
        SysUser user = TokenUtils.getCurrentUser();
        rateLimitService.check("risk", user.getId(), 30);
        return Result.success(riskService.status(user.getId()));
    }
}
