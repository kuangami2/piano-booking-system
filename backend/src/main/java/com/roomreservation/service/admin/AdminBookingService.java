package com.roomreservation.service.admin;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.roomreservation.common.Constants;
import com.roomreservation.entity.Booking;
import com.roomreservation.entity.Room;
import com.roomreservation.entity.SysUser;
import com.roomreservation.exception.ServiceException;
import com.roomreservation.mapper.BookingMapper;
import com.roomreservation.mapper.MessageMapper;
import com.roomreservation.mapper.RoomMapper;
import com.roomreservation.mapper.SysUserMapper;
import com.roomreservation.service.VacancyNotifyService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 管理端预约 Service：按日期/琴房/用户/状态查询、管理员取消并触发空出提醒
 */
@Service
/**
 * 管理端预约业务：条件查询与取消处理。
 */
public class AdminBookingService {

    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final List<String> STATUSES = List.of("booked", "cancelled", "finished");

    @Resource
    private BookingMapper bookingMapper;
    @Resource
    private VacancyNotifyService vacancyNotifyService;
    @Resource
    private RoomMapper roomMapper;
    @Resource
    private SysUserMapper sysUserMapper;

    /**
     * 预约分页查询，可选 date、roomId、userId、status 过滤，附琴房名与用户信息
     */
    public Map<String, Object> page(String date, Integer roomId, Integer userId, String status,
                                    Integer page, Integer size) {
        if (StrUtil.isNotBlank(status) && !STATUSES.contains(status)) {
            throw new ServiceException(Constants.CODE_400, "status 只能为 booked、cancelled 或 finished");
        }
        LambdaQueryWrapper<Booking> wrapper = new LambdaQueryWrapper<>();
        if (StrUtil.isNotBlank(date)) {
            try {
                wrapper.eq(Booking::getBookDate, LocalDate.parse(date));
            } catch (DateTimeParseException e) {
                throw new ServiceException(Constants.CODE_400, "日期格式应为 yyyy-MM-dd");
            }
        }
        if (roomId != null) {
            wrapper.eq(Booking::getRoomId, roomId);
        }
        if (userId != null) {
            wrapper.eq(Booking::getUserId, userId);
        }
        if (StrUtil.isNotBlank(status)) {
            wrapper.eq(Booking::getStatus, status);
        }
        // 最新预约靠前，与用户端口径一致
        wrapper.orderByDesc(Booking::getId);
        Page<Booking> result = bookingMapper.selectPage(new Page<>(page, size), wrapper);
        List<Booking> records = result.getRecords();

        Set<Integer> userIds = new HashSet<>();
        Set<Integer> roomIds = new HashSet<>();
        for (Booking b : records) {
            userIds.add(b.getUserId());
            roomIds.add(b.getRoomId());
        }
        Map<Integer, SysUser> users = new HashMap<>();
        if (!userIds.isEmpty()) {
            for (SysUser u : sysUserMapper.selectBatchIds(userIds)) {
                users.put(u.getId(), u);
            }
        }
        Map<Integer, Room> rooms = new HashMap<>();
        if (!roomIds.isEmpty()) {
            for (Room r : roomMapper.selectBatchIds(roomIds)) {
                rooms.put(r.getId(), r);
            }
        }
        List<Map<String, Object>> rows = new ArrayList<>();
        for (Booking b : records) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", b.getId());
            row.put("userId", b.getUserId());
            row.put("roomId", b.getRoomId());
            row.put("bookDate", b.getBookDate() == null ? null : b.getBookDate().toString());
            row.put("startMin", b.getStartMin());
            row.put("endMin", b.getEndMin());
            row.put("status", b.getStatus());
            row.put("createdAt", b.getCreatedAt() == null ? null : b.getCreatedAt().format(DATE_TIME));
            Room room = rooms.get(b.getRoomId());
            row.put("roomName", room == null ? null : room.getName());
            row.put("roomType", room == null ? null : room.getRoomType());
            SysUser user = users.get(b.getUserId());
            row.put("username", user == null ? null : user.getUsername());
            row.put("userName", user == null ? null : user.getName());
            rows.add(row);
        }
        Map<String, Object> data = new HashMap<>();
        data.put("list", rows);
        data.put("total", result.getTotal());
        return data;
    }

    /**
     * 管理员取消预约，仅 booked 可取消，成功后释放时段并通知关注者
     */
    @Transactional(rollbackFor = Exception.class)
    public void cancel(Integer bookingId) {
        Booking booking = bookingId == null ? null : bookingMapper.selectById(bookingId);
        if (booking == null) {
            throw new ServiceException(Constants.CODE_404, "预约记录不存在");
        }
        if (!"booked".equals(booking.getStatus())) {
            throw new ServiceException(Constants.CODE_400, "该预约已取消或已完成，不能重复取消");
        }
        bookingMapper.update(null, new LambdaUpdateWrapper<Booking>()
                .eq(Booking::getId, bookingId)
                .set(Booking::getStatus, "cancelled"));
        // 提醒关注者空出，同时通知预约人本人的预约已被取消
        vacancyNotifyService.notifyWatchers(booking);
        vacancyNotifyService.notifyCancelled(booking, "admin");
    }
}
