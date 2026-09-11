package com.roomreservation.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.roomreservation.entity.Booking;

import java.util.List;

/**
 * 预约 Service，预约校验、冲突检测、退约与空出提醒
 */
public interface IBookingService extends IService<Booking> {

    /**
     * 创建预约，规则校验与冲突检测，冲突抛 409，返回写入后的预约记录含主键
     */
    Booking createBooking(Booking booking);

    /**
     * 退约取消，仅本人有效预约可退，触发该时段关注者的空出提醒
     */
    void cancelBooking(Integer userId, Integer bookingId);
}
