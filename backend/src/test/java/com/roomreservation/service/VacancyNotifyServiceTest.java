package com.roomreservation.service;

import com.roomreservation.entity.Message;
import com.roomreservation.entity.Watch;
import com.roomreservation.event.BookingCancelledPayload;
import com.roomreservation.mapper.MessageMapper;
import com.roomreservation.mapper.RoomMapper;
import com.roomreservation.mapper.WatchMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 空出提醒单元测试：区间匹配写消息并置关注状态，无关注者与非法载荷直接返回。
 */
@ExtendWith(MockitoExtension.class)
class VacancyNotifyServiceTest {

    @Mock private WatchMapper watchMapper;
    @Mock private MessageMapper messageMapper;
    @Mock private RoomMapper roomMapper;

    @InjectMocks private VacancyNotifyService service;

    private BookingCancelledPayload payload() {
        return new BookingCancelledPayload(5, 1, 10, "B101", "2026-09-16", 600, 690);
    }

    @Test
    @DisplayName("匹配到关注者时写站内消息并置为已提醒")
    void notifiesMatchedWatchers() {
        Watch watch = new Watch();
        watch.setId(3);
        watch.setUserId(2);
        when(watchMapper.selectList(any())).thenReturn(List.of(watch));

        int count = service.notifyWatchers(payload());

        assertThat(count).isEqualTo(1);
        verify(messageMapper).insert(any(Message.class));
        verify(watchMapper).update(isNull(), any());
    }

    @Test
    @DisplayName("无关注者时不写消息")
    void skipsWhenNoWatchers() {
        when(watchMapper.selectList(any())).thenReturn(List.of());

        int count = service.notifyWatchers(payload());

        assertThat(count).isZero();
        verify(messageMapper, never()).insert(any(Message.class));
    }

    @Test
    @DisplayName("管理员取消时给预约人发取消确认")
    void notifiesOwnerWhenCancelledByAdmin() {
        ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);

        int sent = service.notifyCancelled(payload(), "admin");

        assertThat(sent).isEqualTo(1);
        verify(messageMapper).insert(captor.capture());
        assertThat(captor.getValue().getUserId()).isEqualTo(1);
        assertThat(captor.getValue().getMsgType()).isEqualTo("booking_cancelled");
        assertThat(captor.getValue().getContent()).contains("已被管理员取消").contains("B101");
    }

    @Test
    @DisplayName("本人退约时给预约人发退约确认")
    void notifiesOwnerOnSelfCancel() {
        ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);

        service.notifyCancelled(payload(), "self");

        verify(messageMapper).insert(captor.capture());
        assertThat(captor.getValue().getContent()).contains("已退约成功");
    }

    @Test
    @DisplayName("载荷为空时取消确认直接返回")
    void skipsCancelledNoticeWithoutPayload() {
        assertThat(service.notifyCancelled((BookingCancelledPayload) null, "self")).isZero();
    }

    @Test
    @DisplayName("载荷缺少房间或日期时直接返回")
    void returnsZeroOnIncompletePayload() {
        assertThat(service.notifyWatchers((BookingCancelledPayload) null)).isZero();
        assertThat(service.notifyWatchers(
                new BookingCancelledPayload(5, 1, null, "B101", "2026-09-16", 600, 690))).isZero();
    }
}
