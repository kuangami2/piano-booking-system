package com.roomreservation.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;

/**
 * 用户表
 */
@Data
@TableName("sys_user")
public class SysUser {

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    /** 登录名 */
    private String username;

    /** 密码，BCrypt 加密 */
    private String password;

    /** 姓名 */
    private String name;

    /** 学号 */
    private String studentNo;

    /** 邮箱 */
    private String email;

    /** 角色，user 或 admin */
    private String role;

    /** 是否会员，会员可预约对内琴房 */
    private Boolean isMember;

    /** 信用分 */
    private Integer credit;

    /** 微信 openid，预留 */
    private String openid;

    /** 状态，normal 或 banned */
    private String status;

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    /** 非表字段，改密时携带新密码 */
    @TableField(exist = false)
    private String newPassword;
}
