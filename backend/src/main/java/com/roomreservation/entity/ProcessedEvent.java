package com.roomreservation.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;

/**
 * 消费幂等表：以 eventId 为主键，重复投递直接丢弃
 */
@Data
@TableName("processed_event")
public class ProcessedEvent {

    @TableId(value = "event_id", type = IdType.INPUT)
    private String eventId;

    private String eventType;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime handledAt;
}
