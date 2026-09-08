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
 * 预约表
 */
@Data
@TableName("booking")
public class Booking {

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    private Integer userId;

    private Integer roomId;

    /** 预约日期 */
    private LocalDate bookDate;

    /** 开始分钟 */
    private Integer startMin;

    /** 结束分钟 */
    private Integer endMin;

    /** 状态，booked、cancelled 或 finished */
    private String status;

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
    /** 非表字段，列表展示用琴房名称 */
    @TableField(exist = false)
    private String roomName;
}
