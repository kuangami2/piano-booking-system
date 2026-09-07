package com.roomreservation.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 乐器表，琴房一对多
 */
@Data
@TableName("instrument")
public class Instrument {

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    /** 所属琴房 */
    private Integer roomId;

    private String name;

    /** 数量 */
    private Integer count;

    private String note;
}
