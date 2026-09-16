package com.roomreservation.service.admin;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.roomreservation.common.Constants;
import com.roomreservation.entity.Booking;
import com.roomreservation.exception.ServiceException;
import com.roomreservation.mapper.BookingMapper;
import com.roomreservation.mapper.RoomMapper;
import com.roomreservation.mapper.SysUserMapper;
import com.roomreservation.service.VacancyNotifyService;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 管理端取消预约单元测试：状态校验与通知范围，含给预约人的取消确认。
 */
@ExtendWith(MockitoExtension.class)
class AdminBookingServiceTest {

    @Mock private BookingMapper bookingMapper;
    @Mock private RoomMapper roomMapper;
    @Mock private SysUserMapper sysUserMapper;
    @Mock private VacancyNotifyService vacancyNotifyService;

    @InjectMocks private AdminBookingService service;

    /** 纯单元测试没有 Spring 上下文，LambdaUpdateWrapper 的 set 需要实体元数据 */
    @BeforeAll
    static void initMybatisPlusTableInfo() {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        TableInfoHelper.initTableInfo(assistant, Booking.class);
    }

    private Booking booked() {
        Booking booking = new Booking();
        booking.setId(9);
        booking.setUserId(1);
        booking.setRoomId(10);
        booking.setBookDate(LocalDate.now().plusDays(1));
        booking.setStartMin(600);
        booking.setEndMin(690);
        booking.setStatus("booked");
        return booking;
    }

    @Test
    @DisplayName("取消预约置为已取消，并通知关注者与预约人本人")
    void cancelsAndNotifiesBothSides() {
        Booking booking = booked();
        when(bookingMapper.selectById(9)).thenReturn(booking);

        service.cancel(9);

        verify(bookingMapper).update(isNull(), any());
        verify(vacancyNotifyService).notifyWatchers(booking);
        verify(vacancyNotifyService).notifyCancelled(booking, "admin");
    }

    @Test
    @DisplayName("非进行中预约不能重复取消")
    void rejectsNonBooked() {
        Booking booking = booked();
        booking.setStatus("cancelled");
        when(bookingMapper.selectById(9)).thenReturn(booking);

        assertThatThrownBy(() -> service.cancel(9))
                .isInstanceOf(ServiceException.class)
                .hasFieldOrPropertyWithValue("code", Constants.CODE_400);
    }

    @Test
    @DisplayName("预约不存在返回 404")
    void rejectsMissingBooking() {
        when(bookingMapper.selectById(9)).thenReturn(null);

        assertThatThrownBy(() -> service.cancel(9))
                .isInstanceOf(ServiceException.class)
                .hasFieldOrPropertyWithValue("code", Constants.CODE_404);
    }

    @Test
    @DisplayName("预约 ID 为空返回 404")
    void rejectsNullId() {
        assertThatThrownBy(() -> service.cancel(null))
                .isInstanceOf(ServiceException.class)
                .hasFieldOrPropertyWithValue("code", Constants.CODE_404);
    }
}
