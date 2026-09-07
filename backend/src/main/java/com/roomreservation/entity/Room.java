package com.roomreservation.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 琴房表
 */
@Data
@TableName("room")
public class Room {

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    private String name;

    /** 类型，inner 对内或 outer 对外 */
    private String roomType;

    /** 可约窗起始分钟，360 表示 6 点 */
    private Integer openStart;

    /** 可约窗结束分钟，1320 表示 22 点 */
    private Integer openEnd;

    /** 琴房说明与注意事项 */
    private String description;

    /** 状态，normal 或停用 */
    private String status;
}
