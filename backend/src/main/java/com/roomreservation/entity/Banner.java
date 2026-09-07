package com.roomreservation.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 轮播图表
 */
@Data
@TableName("banner")
public class Banner {

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    /** 图片地址 */
    private String image;

    /** 跳转链接 */
    private String link;

    /** 排序 */
    private Integer sort;

    /** 状态 */
    private String status;
}
