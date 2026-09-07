package com.roomreservation.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;

/**
 * 信用流水表
 */
@Data
@TableName("credit_log")
public class CreditLog {

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    private Integer userId;

    /** 变动值，负为扣分 */
    private Integer changeValue;

    private String reason;

    /** 经办管理员 */
    private Integer operatorId;

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
}
