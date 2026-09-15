package com.roomreservation.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.roomreservation.common.Constants;
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
import com.roomreservation.service.ActivityService;
import com.roomreservation.service.CacheService;
import com.roomreservation.service.IRuleConfigService;
import com.roomreservation.service.ISysUserService;
import com.roomreservation.service.RiskService;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 预约业务单元测试：规则校验各分支、并发冲突兜底与退约提醒。
 * 规则参数统一走 getInt 的默认值，即 30 分钟粒度、240 分钟上限、7 天提前、3 次周预约、2 次周退约、60 分信用阈值。
 */
@ExtendWith(MockitoExtension.class)
class BookingServiceImplTest {

    @Mock private ISysUserService sysUserService;
    @Mock private RoomMapper roomMapper;
    @Mock private BookingMapper bookingMapper;
    @Mock private WatchMapper watchMapper;
    @Mock private MessageMapper messageMapper;
    @Mock private IRuleConfigService ruleConfigService;
    @Mock private CacheService cacheService;
    @Mock private RiskService riskService;
    @Mock private ActivityService activityService;

    @InjectMocks private BookingServiceImpl service;

    /**
     * 纯单元测试没有 Spring 上下文，LambdaUpdateWrapper 的 set 需要实体元数据，
     * 这里手动注册 Booking 与 Watch 两个会被 set 的实体。
     */
    @BeforeAll
    static void initMybatisPlusTableInfo() {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        TableInfoHelper.initTableInfo(assistant, Booking.class);
        TableInfoHelper.initTableInfo(assistant, Watch.class);
    }

    private LocalDate tomorrow;

    @BeforeEach
    void setUp() {
        tomorrow = LocalDate.now().plusDays(1);
        lenient().when(ruleConfigService.getInt(anyString(), anyInt()))
                .thenAnswer(invocation -> invocation.getArgument(1));
    }

    private SysUser user(int credit, boolean member, String role) {
        SysUser u = new SysUser();
        u.setId(1);
        u.setUsername("demo");
        u.setRole(role);
        u.setIsMember(member);
        u.setCredit(credit);
        return u;
    }

    private Room room(String type) {
        Room r = new Room();
        r.setId(10);
        r.setName("B101");
        r.setRoomType(type);
        r.setOpenStart(360);
        r.setOpenEnd(1320);
        return r;
    }

    private Booking request(LocalDate date, int startMin, int endMin) {
        Booking b = new Booking();
        b.setUserId(1);
        b.setRoomId(10);
        b.setBookDate(date);
        b.setStartMin(startMin);
        b.setEndMin(endMin);
        return b;
    }

    private Booking existingBooking() {
        Booking b = new Booking();
        b.setId(5);
        b.setUserId(1);
        b.setRoomId(10);
        b.setBookDate(tomorrow);
        b.setStartMin(600);
        b.setEndMin(690);
        b.setStatus("booked");
        return b;
    }

    private void givenBookableRoom() {
        when(sysUserService.getById(1)).thenReturn(user(100, false, "user"));
        when(roomMapper.selectById(10)).thenReturn(room("outer"));
    }

    @Test
    @DisplayName("规则全部通过时写入预约并失效缓存、记活跃度")
    void createsBookingWhenAllRulesPass() {
        givenBookableRoom();
        when(bookingMapper.selectCount(any())).thenReturn(0L);

        Booking result = service.createBooking(request(tomorrow, 600, 690));

        assertThat(result.getStatus()).isEqualTo("booked");
        assertThat(result.getUserId()).isEqualTo(1);
        assertThat(result.getStartMin()).isEqualTo(600);
        assertThat(result.getEndMin()).isEqualTo(690);
        verify(roomMapper).lockRoom(10);
        verify(bookingMapper).insert(any(Booking.class));
        verify(cacheService).evict("free:10:" + tomorrow);
        verify(activityService).record(1, "booking");
    }

    @Test
    @DisplayName("参数不完整直接拒绝")
    void rejectsIncompleteRequest() {
        assertThatThrownBy(() -> service.createBooking(new Booking()))
                .isInstanceOf(ServiceException.class)
                .hasFieldOrPropertyWithValue("code", Constants.CODE_400);
    }

    @Test
    @DisplayName("结束时间不晚于开始时间直接拒绝")
    void rejectsInvalidTimeRange() {
        assertThatThrownBy(() -> service.createBooking(request(tomorrow, 690, 690)))
                .isInstanceOf(ServiceException.class)
                .hasFieldOrPropertyWithValue("code", Constants.CODE_400);
    }

    @Test
    @DisplayName("用户不存在返回 401")
    void rejectsUnknownUser() {
        when(sysUserService.getById(1)).thenReturn(null);

        assertThatThrownBy(() -> service.createBooking(request(tomorrow, 600, 690)))
                .isInstanceOf(ServiceException.class)
                .hasFieldOrPropertyWithValue("code", Constants.CODE_401);
    }

    @Test
    @DisplayName("信用分低于阈值暂停预约")
    void rejectsLowCredit() {
        when(sysUserService.getById(1)).thenReturn(user(50, false, "user"));

        assertThatThrownBy(() -> service.createBooking(request(tomorrow, 600, 690)))
                .isInstanceOf(ServiceException.class)
                .hasFieldOrPropertyWithValue("code", Constants.CODE_409);
    }

    @Test
    @DisplayName("非会员不能预约对内琴房")
    void rejectsInnerRoomForNonMember() {
        when(sysUserService.getById(1)).thenReturn(user(100, false, "user"));
        when(roomMapper.selectById(10)).thenReturn(room("inner"));

        assertThatThrownBy(() -> service.createBooking(request(tomorrow, 600, 690)))
                .isInstanceOf(ServiceException.class)
                .hasFieldOrPropertyWithValue("code", Constants.CODE_403);
    }

    @Test
    @DisplayName("会员可预约对内琴房")
    void allowsInnerRoomForMember() {
        when(sysUserService.getById(1)).thenReturn(user(100, true, "user"));
        when(roomMapper.selectById(10)).thenReturn(room("inner"));
        when(bookingMapper.selectCount(any())).thenReturn(0L);

        Booking result = service.createBooking(request(tomorrow, 600, 690));

        assertThat(result.getStatus()).isEqualTo("booked");
    }

    @Test
    @DisplayName("不能预约过去的日期")
    void rejectsPastDate() {
        givenBookableRoom();

        assertThatThrownBy(() -> service.createBooking(request(LocalDate.now().minusDays(1), 600, 690)))
                .isInstanceOf(ServiceException.class)
                .hasFieldOrPropertyWithValue("code", Constants.CODE_400);
    }

    @Test
    @DisplayName("不能超出提前预约天数")
    void rejectsBeyondAdvanceDays() {
        givenBookableRoom();

        assertThatThrownBy(() -> service.createBooking(request(LocalDate.now().plusDays(8), 600, 690)))
                .isInstanceOf(ServiceException.class)
                .hasFieldOrPropertyWithValue("code", Constants.CODE_400);
    }

    @Test
    @DisplayName("不能超出琴房可约时间窗")
    void rejectsOutOfOpenWindow() {
        givenBookableRoom();

        assertThatThrownBy(() -> service.createBooking(request(tomorrow, 300, 690)))
                .isInstanceOf(ServiceException.class)
                .hasFieldOrPropertyWithValue("code", Constants.CODE_400);
    }

    @Test
    @DisplayName("未按最小单位对齐直接拒绝")
    void rejectsMisalignedSlot() {
        givenBookableRoom();

        assertThatThrownBy(() -> service.createBooking(request(tomorrow, 601, 690)))
                .isInstanceOf(ServiceException.class)
                .hasFieldOrPropertyWithValue("code", Constants.CODE_400);
    }

    @Test
    @DisplayName("超过单次时长上限直接拒绝")
    void rejectsTooLongBooking() {
        givenBookableRoom();

        assertThatThrownBy(() -> service.createBooking(request(tomorrow, 600, 900)))
                .isInstanceOf(ServiceException.class)
                .hasFieldOrPropertyWithValue("code", Constants.CODE_400);
    }

    @Test
    @DisplayName("本周预约次数达上限返回 409")
    void rejectsWeeklyLimit() {
        givenBookableRoom();
        when(bookingMapper.selectCount(any())).thenReturn(3L);

        assertThatThrownBy(() -> service.createBooking(request(tomorrow, 600, 690)))
                .isInstanceOf(ServiceException.class)
                .hasFieldOrPropertyWithValue("code", Constants.CODE_409);
    }

    @Test
    @DisplayName("时段冲突返回 409")
    void rejectsOverlappingSlot() {
        givenBookableRoom();
        when(bookingMapper.selectCount(any())).thenReturn(0L, 1L);

        assertThatThrownBy(() -> service.createBooking(request(tomorrow, 600, 690)))
                .isInstanceOf(ServiceException.class)
                .hasFieldOrPropertyWithValue("code", Constants.CODE_409);
    }

    @Test
    @DisplayName("唯一键兜底命中等同并发冲突")
    void mapsDuplicateKeyToConflict() {
        givenBookableRoom();
        when(bookingMapper.selectCount(any())).thenReturn(0L);
        when(bookingMapper.insert(any(Booking.class))).thenThrow(new DuplicateKeyException("uk_book_slot"));

        assertThatThrownBy(() -> service.createBooking(request(tomorrow, 600, 690)))
                .isInstanceOf(ServiceException.class)
                .hasFieldOrPropertyWithValue("code", Constants.CODE_409);
    }

    @Test
    @DisplayName("退约成功置为已取消并失效缓存、记负分")
    void cancelsOwnBooking() {
        when(bookingMapper.selectById(5)).thenReturn(existingBooking());
        when(bookingMapper.selectCount(any())).thenReturn(0L);
        when(watchMapper.selectList(any())).thenReturn(List.of());

        service.cancelBooking(1, 5);

        verify(bookingMapper).update(isNull(), any());
        verify(cacheService).evict("free:10:" + tomorrow);
        verify(riskService).onCancel(any(Booking.class));
        verify(activityService).record(1, "cancel");
        verify(messageMapper, never()).insert(any(Message.class));
    }

    @Test
    @DisplayName("退约的记录不存在返回 404")
    void rejectsMissingBooking() {
        when(bookingMapper.selectById(9)).thenReturn(null);

        assertThatThrownBy(() -> service.cancelBooking(1, 9))
                .isInstanceOf(ServiceException.class)
                .hasFieldOrPropertyWithValue("code", Constants.CODE_404);
    }

    @Test
    @DisplayName("退约不是本人记录返回 403")
    void rejectsCancellingOthersBooking() {
        Booking booking = existingBooking();
        booking.setUserId(2);
        when(bookingMapper.selectById(5)).thenReturn(booking);

        assertThatThrownBy(() -> service.cancelBooking(1, 5))
                .isInstanceOf(ServiceException.class)
                .hasFieldOrPropertyWithValue("code", Constants.CODE_403);
    }

    @Test
    @DisplayName("重复退约返回 400")
    void rejectsDuplicateCancel() {
        Booking booking = existingBooking();
        booking.setStatus("cancelled");
        when(bookingMapper.selectById(5)).thenReturn(booking);

        assertThatThrownBy(() -> service.cancelBooking(1, 5))
                .isInstanceOf(ServiceException.class)
                .hasFieldOrPropertyWithValue("code", Constants.CODE_400);
    }

    @Test
    @DisplayName("本周退约次数达上限返回 409")
    void rejectsWeeklyCancelLimit() {
        when(bookingMapper.selectById(5)).thenReturn(existingBooking());
        when(bookingMapper.selectCount(any())).thenReturn(2L);

        assertThatThrownBy(() -> service.cancelBooking(1, 5))
                .isInstanceOf(ServiceException.class)
                .hasFieldOrPropertyWithValue("code", Constants.CODE_409);
    }

    @Test
    @DisplayName("退约后向关注用户发站内提醒并置为已提醒")
    void notifiesWatchersAfterCancel() {
        Watch watch = new Watch();
        watch.setId(3);
        watch.setUserId(2);
        when(bookingMapper.selectById(5)).thenReturn(existingBooking());
        when(bookingMapper.selectCount(any())).thenReturn(0L);
        when(watchMapper.selectList(any())).thenReturn(List.of(watch));
        when(roomMapper.selectById(10)).thenReturn(room("outer"));

        service.cancelBooking(1, 5);

        verify(messageMapper).insert(any(Message.class));
        verify(watchMapper).update(isNull(), any());
    }
}
