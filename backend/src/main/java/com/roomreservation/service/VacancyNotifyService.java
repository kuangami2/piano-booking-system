package com.roomreservation.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.roomreservation.entity.Booking;
import com.roomreservation.entity.Message;
import com.roomreservation.entity.Room;
import com.roomreservation.entity.Watch;
import com.roomreservation.event.BookingCancelledPayload;
import com.roomreservation.mapper.MessageMapper;
import com.roomreservation.mapper.RoomMapper;
import com.roomreservation.mapper.WatchMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 空出提醒：退约后按区间重叠匹配关注者，写站内消息并置关注状态为已提醒。
 * 异步消费者与同步降级路径共用本服务，消息内容口径一致。
 */
@Service
public class VacancyNotifyService {

    @Resource
    private WatchMapper watchMapper;
    @Resource
    private MessageMapper messageMapper;
    @Resource
    private RoomMapper roomMapper;

    /** 退约实体入口，琴房名缺失时回查 */
    public int notifyWatchers(Booking booking) {
        Room room = roomMapper.selectById(booking.getRoomId());
        String roomName = room == null ? "琴房" : room.getName();
        BookingCancelledPayload payload = new BookingCancelledPayload(
                booking.getId(), booking.getUserId(), booking.getRoomId(), roomName,
                booking.getBookDate() == null ? null : booking.getBookDate().toString(),
                booking.getStartMin(), booking.getEndMin());
        return notifyWatchers(payload);
    }

    /** 事件载荷入口，消费者用快照里的琴房名，不再回查 */
    public int notifyWatchers(BookingCancelledPayload payload) {
        if (payload == null || payload.getRoomId() == null || payload.getBookDate() == null) {
            return 0;
        }
        List<Watch> watchers = watchMapper.selectList(new LambdaQueryWrapper<Watch>()
                .eq(Watch::getRoomId, payload.getRoomId())
                .eq(Watch::getBookDate, java.time.LocalDate.parse(payload.getBookDate()))
                .eq(Watch::getStatus, "active")
                .ne(Watch::getUserId, payload.getUserId())
                .lt(Watch::getStartMin, payload.getEndMin())
                .gt(Watch::getEndMin, payload.getStartMin()));
        if (watchers.isEmpty()) {
            return 0;
        }
        String roomName = payload.getRoomName() == null ? "琴房" : payload.getRoomName();
        String content = "你关注的 " + roomName + " " + payload.getBookDate()
                + " " + minToTime(payload.getStartMin()) + "-" + minToTime(payload.getEndMin())
                + " 已空出，请尽快预约";
        for (Watch w : watchers) {
            Message msg = new Message();
            msg.setUserId(w.getUserId());
            msg.setMsgType("vacancy");
            msg.setContent(content);
            msg.setIsRead(false);
            messageMapper.insert(msg);
            watchMapper.update(null, new LambdaUpdateWrapper<Watch>()
                    .eq(Watch::getId, w.getId())
                    .set(Watch::getStatus, "notified"));
        }
        return watchers.size();
    }

    private String minToTime(Integer minutes) {
        int value = minutes == null ? 0 : minutes;
        return String.format("%02d:%02d", value / 60, value % 60);
    }
}
