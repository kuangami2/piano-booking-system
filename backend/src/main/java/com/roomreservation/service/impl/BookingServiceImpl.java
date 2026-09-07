package com.roomreservation.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.roomreservation.common.Constants;
import com.roomreservation.common.RuleKeys;
import com.roomreservation.entity.Booking;
import com.roomreservation.entity.Message;
import com.roomreservation.entity.Room;
import com.roomreservation.entity.SysUser;
import com.roomreservation.entity.Watch;
import com.roomreservation.exception.ServiceException;
import com.roomreservation.mapper.BookingMapper;
import com.roomreservation.mapper.MessageMapper;
import com.roomreservation.mapper.RoomMapper;
import com.roomreservation.mapper.WatchMapper;
import com.roomreservation.service.IBookingService;
import com.roomreservation.service.IRuleConfigService;
import com.roomreservation.service.ISysUserService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.List;

/**
 * 预约 Service 实现
 */
@Service
public class BookingServiceImpl extends ServiceImpl<BookingMapper, Booking> implements IBookingService {

    @Resource
    private ISysUserService sysUserService;
    @Resource
    private RoomMapper roomMapper;
    @Resource
    private BookingMapper bookingMapper;
    @Resource
    private WatchMapper watchMapper;
    @Resource
    private MessageMapper messageMapper;
    @Resource
    private IRuleConfigService ruleConfigService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createBooking(Booking booking) {
        Integer userId = booking.getUserId();
        Integer roomId = booking.getRoomId();
        LocalDate bookDate = booking.getBookDate();
        Integer startMin = booking.getStartMin();
        Integer endMin = booking.getEndMin();
        if (userId == null || roomId == null || bookDate == null || startMin == null || endMin == null) {
            throw new ServiceException(Constants.CODE_400, "预约参数不完整");
        }
        if (startMin >= endMin) {
            throw new ServiceException(Constants.CODE_400, "结束时间必须晚于开始时间");
        }
        SysUser user = sysUserService.getById(userId);
        if (user == null) {
            throw new ServiceException(Constants.CODE_401, "用户不存在，请重新登录");
        }
        // 规则快照
        int minUnit = ruleConfigService.getInt(RuleKeys.BOOKING_MIN_UNIT, 30);
        int maxDuration = ruleConfigService.getInt(RuleKeys.BOOKING_MAX_DURATION, 240);
        int advanceDays = ruleConfigService.getInt(RuleKeys.BOOKING_ADVANCE_DAYS, 7);
        int weeklyLimit = ruleConfigService.getInt(RuleKeys.BOOKING_WEEKLY_LIMIT, 3);
        int lowThreshold = ruleConfigService.getInt(RuleKeys.CREDIT_LOW, 60);
        // 信用暂停
        if (user.getCredit() < lowThreshold) {
            throw new ServiceException(Constants.CODE_409, "信用分过低，预约服务已暂停，请联系管理员");
        }
        Room room = roomMapper.selectById(roomId);
        if (room == null) {
            throw new ServiceException(Constants.CODE_400, "琴房不存在");
        }
        boolean admin = "admin".equals(user.getRole());
        boolean member = Boolean.TRUE.equals(user.getIsMember());
        // 对内琴房权限
        if ("inner".equals(room.getRoomType()) && !admin && !member) {
            throw new ServiceException(Constants.CODE_403, "对内琴房仅会员可预约");
        }
        // 日期范围
        LocalDate today = LocalDate.now();
        if (bookDate.isBefore(today)) {
            throw new ServiceException(Constants.CODE_400, "不能预约过去的日期");
        }
        if (bookDate.isAfter(today.plusDays(advanceDays))) {
            throw new ServiceException(Constants.CODE_400, "最多提前 " + advanceDays + " 天预约");
        }
        // 时间窗与粒度
        if (startMin < room.getOpenStart() || endMin > room.getOpenEnd()) {
            throw new ServiceException(Constants.CODE_400, "预约时段超出琴房可约时间窗");
        }
        if (startMin % minUnit != 0 || endMin % minUnit != 0) {
            throw new ServiceException(Constants.CODE_400, "预约时间需按 " + minUnit + " 分钟对齐");
        }
        if (endMin - startMin > maxDuration) {
            throw new ServiceException(Constants.CODE_400, "单次预约最长 " + maxDuration / 60 + " 小时");
        }
        // 每周预约次数，自然周从周一起
        LocalDate weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        Long weeklyCount = bookingMapper.selectCount(new LambdaQueryWrapper<Booking>()
                .eq(Booking::getUserId, userId)
                .eq(Booking::getStatus, "booked")
                .ge(Booking::getBookDate, weekStart));
        if (weeklyCount >= weeklyLimit) {
            throw new ServiceException(Constants.CODE_409, "本周预约已达 " + weeklyLimit + " 次上限");
        }
        // 锁房间行串行化同房写，再查重叠冲突
        roomMapper.lockRoom(roomId);
        Long conflict = bookingMapper.selectCount(new LambdaQueryWrapper<Booking>()
                .eq(Booking::getRoomId, roomId)
                .eq(Booking::getBookDate, bookDate)
                .eq(Booking::getStatus, "booked")
                .lt(Booking::getStartMin, endMin)
                .gt(Booking::getEndMin, startMin));
        if (conflict > 0) {
            throw new ServiceException(Constants.CODE_409, "该时段已被预约，请选择其他时段");
        }
        Booking target = new Booking();
        target.setUserId(userId);
        target.setRoomId(roomId);
        target.setBookDate(bookDate);
        target.setStartMin(startMin);
        target.setEndMin(endMin);
        target.setStatus("booked");
        bookingMapper.insert(target);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelBooking(Integer userId, Integer bookingId) {
        Booking booking = bookingMapper.selectById(bookingId);
        if (booking == null) {
            throw new ServiceException(Constants.CODE_404, "预约记录不存在");
        }
        if (!booking.getUserId().equals(userId)) {
            throw new ServiceException(Constants.CODE_403, "只能取消自己的预约");
        }
        if (!"booked".equals(booking.getStatus())) {
            throw new ServiceException(Constants.CODE_400, "该预约已取消或已完成，不能重复退约");
        }
        int cancelLimit = ruleConfigService.getInt(RuleKeys.BOOKING_WEEKLY_CANCEL_LIMIT, 2);
        LocalDate weekStart = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        Long cancelCount = bookingMapper.selectCount(new LambdaQueryWrapper<Booking>()
                .eq(Booking::getUserId, userId)
                .eq(Booking::getStatus, "cancelled")
                .ge(Booking::getBookDate, weekStart));
        if (cancelCount >= cancelLimit) {
            throw new ServiceException(Constants.CODE_409, "本周退约已达 " + cancelLimit + " 次上限");
        }
        bookingMapper.update(null, new LambdaQueryWrapper<Booking>()
                .eq(Booking::getId, bookingId)
                .set(Booking::getStatus, "cancelled"));
        notifyWatchers(booking);
    }

    /**
     * 退约后向关注该时段的用户发站内提醒，先约先得
     */
    private void notifyWatchers(Booking booking) {
        List<Watch> watchers = watchMapper.selectList(new LambdaQueryWrapper<Watch>()
                .eq(Watch::getRoomId, booking.getRoomId())
                .eq(Watch::getBookDate, booking.getBookDate())
                .eq(Watch::getStatus, "active")
                .ne(Watch::getUserId, booking.getUserId())
                .lt(Watch::getStartMin, booking.getEndMin())
                .gt(Watch::getEndMin, booking.getStartMin()));
        if (watchers.isEmpty()) {
            return;
        }
        Room room = roomMapper.selectById(booking.getRoomId());
        String roomName = room == null ? "琴房" : room.getName();
        String content = "你关注的 " + roomName + " " + booking.getBookDate()
                + " " + minToTime(booking.getStartMin()) + "-" + minToTime(booking.getEndMin())
                + " 已空出，请尽快预约";
        for (Watch w : watchers) {
            Message msg = new Message();
            msg.setUserId(w.getUserId());
            msg.setMsgType("vacancy");
            msg.setContent(content);
            msg.setIsRead(false);
            messageMapper.insert(msg);
            watchMapper.update(null, new LambdaQueryWrapper<Watch>()
                    .eq(Watch::getId, w.getId())
                    .set(Watch::getStatus, "notified"));
        }
    }

    private String minToTime(int minutes) {
        return String.format("%02d:%02d", minutes / 60, minutes % 60);
    }
}
