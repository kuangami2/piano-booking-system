package com.roomreservation.controller;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.roomreservation.common.Constants;
import com.roomreservation.common.Result;
import com.fasterxml.jackson.core.type.TypeReference;
import com.roomreservation.common.RuleKeys;
import com.roomreservation.dto.FreeSlot;
import com.roomreservation.entity.Booking;
import com.roomreservation.entity.Instrument;
import com.roomreservation.entity.Room;
import com.roomreservation.entity.SysUser;
import com.roomreservation.exception.ServiceException;
import com.roomreservation.mapper.BookingMapper;
import com.roomreservation.mapper.InstrumentMapper;
import com.roomreservation.mapper.RoomMapper;
import com.roomreservation.service.CacheService;
import com.roomreservation.service.IRuleConfigService;
import com.roomreservation.utils.TokenUtils;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 琴房浏览、详情与空闲查询，登录即可访问，对内琴房仅会员与管理可见可查
 */
@RestController
@RequestMapping("/api/rooms")
/**
 * 琴房接口：列表与详情、空闲时段查询，含角色可见性控制与结果缓存。
 */
public class RoomController {

    @Resource
    private RoomMapper roomMapper;
    @Resource
    private InstrumentMapper instrumentMapper;
    @Resource
    private BookingMapper bookingMapper;
    @Resource
    private IRuleConfigService ruleConfigService;
    @Resource
    private CacheService cacheService;

    /**
     * 琴房列表，参数 type 与 q，普通用户仅见对外琴房
     */
    @GetMapping
    /**
     * 琴房列表：普通用户仅见对外琴房，会员与管理可见对内；
     * 支持类型与关键字筛选与分页，结果按角色与参数维度缓存 60 秒。
     */
    public Result list(@RequestParam(required = false) String type,
                       @RequestParam(required = false) String q,
                       @RequestParam(defaultValue = "1") Integer page,
                       @RequestParam(defaultValue = "10") Integer size) {
        SysUser user = TokenUtils.getCurrentUser();
        boolean privileged = isPrivileged(user);
        LambdaQueryWrapper<Room> wrapper = new LambdaQueryWrapper<>();
        if (StrUtil.isNotBlank(type)) {
            wrapper.eq(Room::getRoomType, type);
        }
        if (!privileged) {
            wrapper.ne(Room::getRoomType, "inner");
        }
        if (StrUtil.isNotBlank(q)) {
            wrapper.like(Room::getName, q);
        }
        wrapper.orderByAsc(Room::getId);
        String cacheKey = "rooms:" + StrUtil.blankToDefault(type, "-") + ":" + StrUtil.blankToDefault(q, "-")
                + ":" + page + ":" + size + ":" + privileged;
        Map<String, Object> cached = cacheService.get(cacheKey, new TypeReference<Map<String, Object>>() {});
        if (cached != null) {
            return Result.success(cached);
        }
        Page<Room> result = roomMapper.selectPage(new Page<>(page, size), wrapper);
        Map<String, Object> data = new HashMap<>();
        data.put("list", result.getRecords());
        data.put("total", result.getTotal());
        cacheService.put(cacheKey, data);
        return Result.success(data);
    }

    /**
     * 琴房详情，含乐器明细与注意事项
     */
    @GetMapping("/{id}")
    /**
     * 琴房详情：返回琴房信息、乐器明细与注意事项，对内琴房校验会员权限。
     */
    public Result detail(@PathVariable Integer id) {
        Room room = roomMapper.selectById(id);
        if (room == null) {
            throw new ServiceException(Constants.CODE_404, "琴房不存在");
        }
        SysUser user = TokenUtils.getCurrentUser();
        if ("inner".equals(room.getRoomType()) && !isPrivileged(user)) {
            throw new ServiceException(Constants.CODE_403, "对内琴房仅会员可查看");
        }
        List<Instrument> instruments = instrumentMapper.selectList(
                new LambdaQueryWrapper<Instrument>().eq(Instrument::getRoomId, id).orderByAsc(Instrument::getId));
        Map<String, Object> data = new HashMap<>();
        data.put("room", room);
        data.put("instruments", instruments);
        return Result.success(data);
    }

    /**
     * 琴房空闲时段查询，返回当日可约连续区间
     */
    @GetMapping("/{id}/free")
    /**
     * 空闲时段查询：以最小单位为块标记已占用的预约区间，再合并连续空闲块为区间返回。
     * 当天只返回当前时刻之后的时段，已过时段与进行中时段不可约；结果缓存 30 秒。
     */
    public Result free(@PathVariable Integer id,
                       @RequestParam String date) {
        Room room = roomMapper.selectById(id);
        if (room == null) {
            throw new ServiceException(Constants.CODE_404, "琴房不存在");
        }
        SysUser user = TokenUtils.getCurrentUser();
        if ("inner".equals(room.getRoomType()) && !isPrivileged(user)) {
            throw new ServiceException(Constants.CODE_403, "对内琴房仅会员可查询");
        }
        LocalDate day;
        try {
            day = LocalDate.parse(date);
        } catch (Exception e) {
            throw new ServiceException(Constants.CODE_400, "日期格式应为 yyyy-MM-dd");
        }
        String freeCacheKey = "free:" + id + ":" + date;
        List<FreeSlot> cachedSlots = cacheService.get(freeCacheKey, new TypeReference<List<FreeSlot>>() {});
        if (cachedSlots != null) {
            return Result.success(cachedSlots);
        }
        int advanceDays = ruleConfigService.getInt(RuleKeys.BOOKING_ADVANCE_DAYS, 7);
        List<FreeSlot> slots = new ArrayList<>();
        LocalDate today = LocalDate.now();
        if (day.isBefore(today) || day.isAfter(today.plusDays(advanceDays))) {
            return Result.success(slots);
        }
        int minUnit = ruleConfigService.getInt(RuleKeys.BOOKING_MIN_UNIT, 30);
        int openStart = room.getOpenStart();
        int openEnd = room.getOpenEnd();
        // 当天只展示当前时刻之后的时段，过期与进行中时段不可约
        if (day.equals(today)) {
            int nowMin = LocalTime.now().getHour() * 60 + LocalTime.now().getMinute();
            int nextSlot = ((nowMin + minUnit - 1) / minUnit) * minUnit;
            openStart = Math.max(openStart, nextSlot);
        }
        int totalSlots = (openEnd - openStart) / minUnit;
        if (totalSlots <= 0) {
            return Result.success(slots);
        }
        // 已约时段按最小单位块标记占用
        boolean[] busyBlock = new boolean[totalSlots];
        List<Booking> bookings = bookingMapper.selectList(new LambdaQueryWrapper<Booking>()
                .eq(Booking::getRoomId, id)
                .eq(Booking::getBookDate, day)
                .eq(Booking::getStatus, "booked")
                .lt(Booking::getStartMin, openEnd)
                .gt(Booking::getEndMin, openStart));
        for (Booking b : bookings) {
            int from = Math.max(b.getStartMin(), openStart);
            int to = Math.min(b.getEndMin(), openEnd);
            int fromIdx = (from - openStart) / minUnit;
            int toIdx = (to - openStart + minUnit - 1) / minUnit;
            for (int i = fromIdx; i < toIdx && i < totalSlots; i++) {
                busyBlock[i] = true;
            }
        }
        // 合并连续空闲块为区间
        int i = 0;
        while (i < totalSlots) {
            if (busyBlock[i]) {
                i++;
                continue;
            }
            int j = i;
            while (j < totalSlots && !busyBlock[j]) {
                j++;
            }
            slots.add(new FreeSlot(openStart + i * minUnit, openStart + j * minUnit));
            i = j;
        }
        cacheService.put(freeCacheKey, slots, 30);
        return Result.success(slots);
    }

    private boolean isPrivileged(SysUser user) {
        if (user == null) {
            return false;
        }
        return "admin".equals(user.getRole()) || Boolean.TRUE.equals(user.getIsMember());
    }
}
