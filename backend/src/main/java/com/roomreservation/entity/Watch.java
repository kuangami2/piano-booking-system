package com.roomreservation.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;

/**
 * 空出提醒关注表
 */
@Data
@TableName("watch")
public class Watch {

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    private Integer userId;

    private Integer roomId;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate bookDate;

    private Integer startMin;

    private Integer endMin;

    /** 状态，active 或 notified */
    private String status;

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
    /** 非表字段，列表展示用琴房名称 */
    @TableField(exist = false)
    private String roomName;
}
