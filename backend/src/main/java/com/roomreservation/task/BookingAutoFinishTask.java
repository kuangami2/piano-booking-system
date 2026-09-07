package com.roomreservation.task;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.roomreservation.entity.Booking;
import com.roomreservation.mapper.BookingMapper;
import jakarta.annotation.Resource;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * 预约时间推进：过期的 booked 预约自动置为 finished，每分钟扫描一次
 */
@Component
public class BookingAutoFinishTask {

    @Resource
    private BookingMapper bookingMapper;

    @Scheduled(fixedDelay = 60000)
    public void autoFinishExpired() {
        bookingMapper.update(null, new LambdaUpdateWrapper<Booking>()
                .eq(Booking::getStatus, "booked")
                .lt(Booking::getBookDate, LocalDate.now())
                .set(Booking::getStatus, "finished"));
    }
}
