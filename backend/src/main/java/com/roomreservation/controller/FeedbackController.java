package com.roomreservation.controller;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.roomreservation.common.Constants;
import com.roomreservation.common.Result;
import com.roomreservation.entity.Feedback;
import com.roomreservation.entity.SysUser;
import com.roomreservation.exception.ServiceException;
import com.roomreservation.mapper.FeedbackMapper;
import com.roomreservation.utils.TokenUtils;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 系统反馈接口
 */
@RestController
@RequestMapping("/api/feedback")
public class FeedbackController {

    @Resource
    private FeedbackMapper feedbackMapper;

    @PostMapping
    public Result submit(@RequestBody Feedback feedback) {
        if (StrUtil.isBlank(feedback.getContent())) {
            throw new ServiceException(Constants.CODE_400, "反馈内容不能为空");
        }
        SysUser user = TokenUtils.getCurrentUser();
        Feedback target = new Feedback();
        target.setUserId(user.getId());
        target.setContent(feedback.getContent());
        target.setStatus("pending");
        feedbackMapper.insert(target);
        return Result.success();
    }

    @GetMapping("/mine")
    public Result mine(@RequestParam(defaultValue = "1") Integer page,
                       @RequestParam(defaultValue = "10") Integer size) {
        SysUser user = TokenUtils.getCurrentUser();
        Page<Feedback> result = feedbackMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<Feedback>()
                        .eq(Feedback::getUserId, user.getId())
                        .orderByDesc(Feedback::getId));
        Map<String, Object> data = new HashMap<>();
        data.put("list", result.getRecords());
        data.put("total", result.getTotal());
        return Result.success(data);
    }
}
