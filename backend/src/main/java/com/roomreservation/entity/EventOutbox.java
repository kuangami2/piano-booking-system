package com.roomreservation.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;

/**
 * 本地消息表：事件与业务同事务落库，提交后投递，失败重投
 */
@Data
@TableName("event_outbox")
public class EventOutbox {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private String eventId;

    private String eventType;

    private String routingKey;

    /** 事件信封 JSON */
    private String payload;

    /** 来源，normal 或 degraded */
    private String source;

    /** 状态，pending 或 sent */
    private String status;

    private Integer retryCount;

    private String lastError;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime sentAt;
}
