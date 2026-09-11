package com.roomreservation.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 登录请求，只校验非空，字段格式限定只作用于注册与改密
 */
@Data
public class LoginRequest {

    @NotBlank(message = "登录名不能为空")
    private String username;

    @NotBlank(message = "密码不能为空")
    private String password;
}
