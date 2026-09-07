package com.roomreservation.service.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.roomreservation.common.Constants;
import com.roomreservation.entity.Booking;
import com.roomreservation.entity.Room;
import com.roomreservation.exception.ServiceException;
import com.roomreservation.mapper.BookingMapper;
import com.roomreservation.mapper.RoomMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 管理端统计 Service：首页概览与琴房使用率明细
 */
@Service
public class AdminStatsService {

    @Resource
    private RoomMapper roomMapper;
    @Resource
    private BookingMapper bookingMapper;

    /**
     * 首页概览：琴房数、今日预约数、本周预约数、预约总人次
     * 今日与本周统计有效预约（booked、finished），总人次为累计全部预约记录
     */
    public Map<String, Object> overview() {
        LocalDate today = LocalDate.now();
        LocalDate weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate weekEnd = weekStart.plusDays(7);
        Long roomCount = roomMapper.selectCount(new LambdaQueryWrapper<Room>()
                .eq(Room::getStatus, "normal"));
        Long todayCount = bookingMapper.selectCount(new LambdaQueryWrapper<Booking>()
                .eq(Booking::getBookDate, today)
                .in(Booking::getStatus, "booked", "finished"));
        Long weekCount = bookingMapper.selectCount(new LambdaQueryWrapper<Booking>()
                .ge(Booking::getBookDate, weekStart)
                .lt(Booking::getBookDate, weekEnd)
                .in(Booking::getStatus, "booked", "finished"));
        Long totalCount = bookingMapper.selectCount(null);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("roomCount", roomCount == null ? 0 : roomCount);
        data.put("todayCount", todayCount == null ? 0 : todayCount);
        data.put("weekCount", weekCount == null ? 0 : weekCount);
        data.put("totalCount", totalCount == null ? 0 : totalCount);
        return data;
    }

    /**
     * 琴房使用率明细：区间内有效预约时长 / 琴房可约窗容量，保留一位小数
     */
    public Map<String, Object> usage(String from, String to) {
        LocalDate fromDate;
        LocalDate toDate;
        try {
            fromDate = LocalDate.parse(from);
            toDate = LocalDate.parse(to);
        } catch (Exception e) {
            throw new ServiceException(Constants.CODE_400, "日期格式应为 yyyy-MM-dd");
        }
        if (fromDate.isAfter(toDate)) {
            throw new ServiceException(Constants.CODE_400, "起始日期不能晚于结束日期");
        }
        long days = ChronoUnit.DAYS.between(fromDate, toDate) + 1;
        if (days > 92) {
            throw new ServiceException(Constants.CODE_400, "统计区间最长 92 天");
        }
        List<Room> rooms = roomMapper.selectList(new LambdaQueryWrapper<Room>().orderByAsc(Room::getId));
        Map<Long, Long> usedByRoom = new HashMap<>();
        if (!rooms.isEmpty()) {
            List<Booking> bookings = bookingMapper.selectList(new LambdaQueryWrapper<Booking>()
                    .ge(Booking::getBookDate, fromDate)
                    .le(Booking::getBookDate, toDate)
                    .in(Booking::getStatus, "booked", "finished"));
            for (Booking b : bookings) {
                int used = b.getEndMin() - b.getStartMin();
                usedByRoom.merge(b.getRoomId().longValue(), (long) Math.max(used, 0), Long::sum);
            }
        }
        List<Map<String, Object>> list = new ArrayList<>();
        for (Room room : rooms) {
            long usedMinutes = usedByRoom.getOrDefault(room.getId().longValue(), 0L);
            long capacity = (long) (room.getOpenEnd() - room.getOpenStart()) * days;
            double rate = capacity <= 0 ? 0
                    : Math.min(100.0, Math.round(usedMinutes * 1000.0 / capacity) / 10.0);
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("roomId", room.getId());
            row.put("name", room.getName());
            row.put("usedMinutes", usedMinutes);
            row.put("rate", rate);
            list.add(row);
        }
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("from", from);
        data.put("to", to);
        data.put("list", list);
        return data;
    }
}
