package com.roomreservation.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;

/**
 * 站内消息表，通知通道抽象
 */
@Data
@TableName("message")
public class Message {

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    private Integer userId;

    /** 类型，如空出提醒 */
    private String msgType;

    private String content;

    /** 是否已读 */
    private Boolean isRead;

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
}
