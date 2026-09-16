package com.roomreservation.controller;

import com.roomreservation.common.Constants;
import com.roomreservation.entity.Booking;
import com.roomreservation.entity.SysUser;
import com.roomreservation.exception.ServiceException;
import com.roomreservation.mapper.BookingMapper;
import com.roomreservation.mapper.RoomMapper;
import com.roomreservation.service.IBookingService;
import com.roomreservation.service.RateLimitService;
import com.roomreservation.utils.TokenUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 预约接口 MockMvc 测试：创建成功、冲突转 409、退约调用服务。
 */
@WebMvcTest(controllers = BookingController.class)
class BookingControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @MockBean
    private IBookingService bookingService;
    @MockBean
    private BookingMapper bookingMapper;
    @MockBean
    private RoomMapper roomMapper;
    @MockBean
    private RateLimitService rateLimitService;

    private static final String SLOT = "{\"roomId\":3,\"bookDate\":\"2026-09-18\",\"startMin\":600,\"endMin\":690}";

    private SysUser currentUser() {
        SysUser user = new SysUser();
        user.setId(7);
        return user;
    }

    @Test
    @DisplayName("创建预约绑定当前登录用户并返回记录")
    void createsBookingForCurrentUser() throws Exception {
        Booking saved = new Booking();
        saved.setId(31);
        saved.setStatus("booked");
        when(bookingService.createBooking(any(Booking.class))).thenReturn(saved);

        try (MockedStatic<TokenUtils> mocked = Mockito.mockStatic(TokenUtils.class)) {
            mocked.when(TokenUtils::getCurrentUser).thenReturn(currentUser());

            mockMvc.perform(post("/api/bookings").contentType(MediaType.APPLICATION_JSON).content(SLOT))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("200"))
                    .andExpect(jsonPath("$.data.id").value(31));
        }
    }

    @Test
    @DisplayName("时段冲突返回 409 业务码")
    void mapsConflictToBusinessCode() throws Exception {
        when(bookingService.createBooking(any(Booking.class)))
                .thenThrow(new ServiceException(Constants.CODE_409, "该时段已被预约，请选择其他时段"));

        try (MockedStatic<TokenUtils> mocked = Mockito.mockStatic(TokenUtils.class)) {
            mocked.when(TokenUtils::getCurrentUser).thenReturn(currentUser());

            mockMvc.perform(post("/api/bookings").contentType(MediaType.APPLICATION_JSON).content(SLOT))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("409"));
        }
    }

    @Test
    @DisplayName("退约调用服务并返回成功")
    void cancelsBooking() throws Exception {
        try (MockedStatic<TokenUtils> mocked = Mockito.mockStatic(TokenUtils.class)) {
            mocked.when(TokenUtils::getCurrentUser).thenReturn(currentUser());

            mockMvc.perform(delete("/api/bookings/31"))
                    .andExpect(jsonPath("$.code").value("200"));
        }
    }
}
