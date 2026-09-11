package com.roomreservation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * 注册请求，字段格式按需求约束，前端同口径即时提示，校验以后端为准
 */
@Data
public class RegisterRequest {

    @NotBlank(message = "登录名不能为空")
    @Pattern(regexp = "^(?:[A-Za-z][A-Za-z0-9_]{3,19}|\\d{10})$",
            message = "登录名为 4 至 20 位字母开头可含字母数字下划线，或 10 位学号")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d)\\S{8,20}$",
            message = "密码为 8 至 20 位且至少含字母与数字，不含空格")
    private String password;

    @NotBlank(message = "姓名不能为空")
    @Pattern(regexp = "^[\\u4e00-\\u9fa5A-Za-z]{2,20}$", message = "姓名为 2 至 20 位汉字或字母")
    private String name;

    @NotBlank(message = "学号不能为空")
    @Pattern(regexp = "^\\d{10}$", message = "学号为 10 位数字")
    private String studentNo;

    @NotBlank(message = "邮箱不能为空")
    @Pattern(regexp = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$", message = "邮箱格式不正确")
    private String email;
}
