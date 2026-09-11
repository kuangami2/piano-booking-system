package com.roomreservation.controller;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.roomreservation.common.Constants;
import com.roomreservation.common.Result;
import com.roomreservation.entity.Room;
import com.roomreservation.entity.SysUser;
import com.roomreservation.entity.Watch;
import com.roomreservation.exception.ServiceException;
import com.roomreservation.mapper.RoomMapper;
import com.roomreservation.mapper.WatchMapper;
import com.roomreservation.service.IWatchService;
import com.roomreservation.service.RateLimitService;
import com.roomreservation.utils.TokenUtils;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 空出提醒关注接口
 */
@RestController
@RequestMapping("/api/watches")
/**
 * 关注接口：新增关注、我的关注列表、取消关注。
 */
public class WatchController {

    @Resource
    private IWatchService watchService;
    @Resource
    private WatchMapper watchMapper;
    @Resource
    private RoomMapper roomMapper;
    @Resource
    private RateLimitService rateLimitService;

    @PostMapping
    public Result add(@RequestBody Watch watch) {
        SysUser user = TokenUtils.getCurrentUser();
        watch.setUserId(user.getId());
        rateLimitService.check("watch", user.getId());
        watchService.addWatch(watch);
        return Result.success();
    }

    @GetMapping("/mine")
    public Result mine(@RequestParam(required = false) String status,
                       @RequestParam(defaultValue = "1") Integer page,
                       @RequestParam(defaultValue = "10") Integer size) {
        SysUser user = TokenUtils.getCurrentUser();
        LambdaQueryWrapper<Watch> wrapper = new LambdaQueryWrapper<Watch>()
                .eq(Watch::getUserId, user.getId());
        if (StrUtil.isNotBlank(status)) {
            wrapper.eq(Watch::getStatus, status);
        }
        wrapper.orderByDesc(Watch::getBookDate).orderByDesc(Watch::getStartMin);
        Page<Watch> result = watchMapper.selectPage(new Page<>(page, size), wrapper);
        fillRoomName(result.getRecords());
        Map<String, Object> data = new HashMap<>();
        data.put("list", result.getRecords());
        data.put("total", result.getTotal());
        return Result.success(data);
    }

    @DeleteMapping("/{id}")
    public Result remove(@PathVariable Integer id) {
        SysUser user = TokenUtils.getCurrentUser();
        Watch watch = watchMapper.selectById(id);
        if (watch == null) {
            throw new ServiceException(Constants.CODE_404, "关注记录不存在");
        }
        if (!watch.getUserId().equals(user.getId())) {
            throw new ServiceException(Constants.CODE_403, "只能取消自己的关注");
        }
        watchMapper.deleteById(id);
        return Result.success();
    }

    private void fillRoomName(List<Watch> records) {
        if (records == null || records.isEmpty()) {
            return;
        }
        List<Integer> roomIds = records.stream().map(Watch::getRoomId).distinct().collect(Collectors.toList());
        Map<Integer, String> names = roomMapper.selectBatchIds(roomIds).stream()
                .collect(Collectors.toMap(Room::getId, Room::getName));
        records.forEach(w -> w.setRoomName(names.get(w.getRoomId())));
    }
}
