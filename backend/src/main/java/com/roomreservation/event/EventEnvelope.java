package com.roomreservation.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 事件统一信封：标识、类型、发生时间、版本与载荷。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EventEnvelope {

    private String eventId;

    private String eventType;

    /** 发生时间，yyyy-MM-dd HH:mm:ss */
    private String occurredAt;

    private Integer version;

    private Object payload;
}
