package com.roomreservation.task;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.roomreservation.entity.Booking;
import com.roomreservation.mapper.BookingMapper;
import jakarta.annotation.Resource;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * 预约时间推进：过期的 booked 预约自动置为 finished，每分钟扫描一次
 */
@Component
/**
 * 定时任务：每分钟把过期与当天已结束的预约置为已完成并释放时段。
 */
public class BookingAutoFinishTask {

    @Resource
    private BookingMapper bookingMapper;

    @Scheduled(fixedDelay = 60000)
    public void autoFinishExpired() {
        LocalDate today = LocalDate.now();
        int nowMin = LocalTime.now().getHour() * 60 + LocalTime.now().getMinute();
        bookingMapper.update(null, new LambdaUpdateWrapper<Booking>()
                .eq(Booking::getStatus, "booked")
                .and(w -> w.lt(Booking::getBookDate, today)
                        .or(o -> o.eq(Booking::getBookDate, today).le(Booking::getEndMin, nowMin)))
                .set(Booking::getStatus, "finished"));
    }
}
