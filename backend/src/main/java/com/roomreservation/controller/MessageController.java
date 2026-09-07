package com.roomreservation.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.roomreservation.common.Constants;
import com.roomreservation.common.Result;
import com.roomreservation.entity.Message;
import com.roomreservation.entity.SysUser;
import com.roomreservation.exception.ServiceException;
import com.roomreservation.mapper.MessageMapper;
import com.roomreservation.utils.TokenUtils;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 站内消息接口
 */
@RestController
@RequestMapping("/api/messages")
public class MessageController {

    @Resource
    private MessageMapper messageMapper;

    @GetMapping
    public Result list(@RequestParam(required = false) Integer unread,
                       @RequestParam(defaultValue = "1") Integer page,
                       @RequestParam(defaultValue = "10") Integer size) {
        SysUser user = TokenUtils.getCurrentUser();
        LambdaQueryWrapper<Message> wrapper = new LambdaQueryWrapper<Message>()
                .eq(Message::getUserId, user.getId());
        if (unread != null && unread == 1) {
            wrapper.eq(Message::getIsRead, false);
        }
        wrapper.orderByDesc(Message::getId);
        Page<Message> result = messageMapper.selectPage(new Page<>(page, size), wrapper);
        Map<String, Object> data = new HashMap<>();
        data.put("list", result.getRecords());
        data.put("total", result.getTotal());
        return Result.success(data);
    }

    @PutMapping("/{id}/read")
    public Result read(@PathVariable Integer id) {
        SysUser user = TokenUtils.getCurrentUser();
        Message message = messageMapper.selectById(id);
        if (message == null) {
            throw new ServiceException(Constants.CODE_404, "消息不存在");
        }
        if (!message.getUserId().equals(user.getId())) {
            throw new ServiceException(Constants.CODE_403, "只能操作自己的消息");
        }
        messageMapper.update(null, new LambdaUpdateWrapper<Message>()
                .eq(Message::getId, id)
                .set(Message::getIsRead, true));
        return Result.success();
    }
}
