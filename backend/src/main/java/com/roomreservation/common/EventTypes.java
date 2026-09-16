package com.roomreservation.common;

/**
 * 阶段 C 事件总线常量：交换机、队列与事件类型。
 */
public interface EventTypes {

    /** 业务事件交换机，topic 类型 */
    String EXCHANGE = "room.events";

    /** 死信交换机 */
    String DLX_EXCHANGE = "room.events.dlx";

    /** 退约空出提醒队列 */
    String VACANCY_QUEUE = "room.notify.vacancy";

    /** 死信队列 */
    String DLQ = "room.events.dlq";

    /** 事件类型与路由键：预约退约 */
    String BOOKING_CANCELLED = "booking.cancelled";

    /** 来源：正常投递 */
    String SOURCE_NORMAL = "normal";

    /** 来源：降级同步 */
    String SOURCE_DEGRADED = "degraded";

    /** 投递状态：待投递 */
    String STATUS_PENDING = "pending";

    /** 投递状态：已投递 */
    String STATUS_SENT = "sent";
}
