package com.roomreservation.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 退约事件载荷：携带完整快照，消费者不回查数据库。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BookingCancelledPayload {

    private Integer bookingId;

    /** 退约人，提醒时排除本人 */
    private Integer userId;

    private Integer roomId;

    private String roomName;

    /** yyyy-MM-dd */
    private String bookDate;

    private Integer startMin;

    private Integer endMin;
}
