package com.roomreservation.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.roomreservation.common.Result;
import com.roomreservation.entity.Booking;
import com.roomreservation.entity.Room;
import com.roomreservation.entity.SysUser;
import com.roomreservation.mapper.BookingMapper;
import com.roomreservation.mapper.RoomMapper;
import com.roomreservation.service.IBookingService;
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

import cn.hutool.core.util.StrUtil;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 用户预约接口：创建、我的预约、退约
 */
@RestController
@RequestMapping("/api/bookings")
/**
 * 预约接口：创建预约、查询我的预约、退约，含限流与风控拦截。
 */
public class BookingController {

    @Resource
    private IBookingService bookingService;
    @Resource
    private BookingMapper bookingMapper;
    @Resource
    private RoomMapper roomMapper;
    @Resource
    private RateLimitService rateLimitService;

    @PostMapping
    public Result create(@RequestBody Booking booking) {
        SysUser user = TokenUtils.getCurrentUser();
        booking.setUserId(user.getId());
        rateLimitService.check("booking", user.getId());
        return Result.success(bookingService.createBooking(booking));
    }

    @GetMapping("/mine")
    public Result mine(@RequestParam(required = false) String status,
                       @RequestParam(defaultValue = "1") Integer page,
                       @RequestParam(defaultValue = "10") Integer size) {
        SysUser user = TokenUtils.getCurrentUser();
        LambdaQueryWrapper<Booking> wrapper = new LambdaQueryWrapper<Booking>()
                .eq(Booking::getUserId, user.getId());
        if (StrUtil.isNotBlank(status)) {
            wrapper.eq(Booking::getStatus, status);
        }
        // 最新预约靠前，按自增主键倒序即创建顺序倒序
        wrapper.orderByDesc(Booking::getId);
        Page<Booking> result = bookingMapper.selectPage(new Page<>(page, size), wrapper);
        fillRoomName(result.getRecords());
        Map<String, Object> data = new HashMap<>();
        data.put("list", result.getRecords());
        data.put("total", result.getTotal());
        return Result.success(data);
    }

    @DeleteMapping("/{id}")
    public Result cancel(@PathVariable Integer id) {
        SysUser user = TokenUtils.getCurrentUser();
        bookingService.cancelBooking(user.getId(), id);
        return Result.success();
    }

    private void fillRoomName(List<Booking> records) {
        if (records == null || records.isEmpty()) {
            return;
        }
        List<Integer> roomIds = records.stream().map(Booking::getRoomId).distinct().collect(Collectors.toList());
        Map<Integer, String> names = roomMapper.selectBatchIds(roomIds).stream()
                .collect(Collectors.toMap(Room::getId, Room::getName));
        records.forEach(b -> b.setRoomName(names.get(b.getRoomId())));
    }
}
