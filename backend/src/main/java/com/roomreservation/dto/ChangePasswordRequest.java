package com.roomreservation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * 修改密码请求，旧密码只校验非空，新密码与注册同口径
 */
@Data
public class ChangePasswordRequest {

    @NotBlank(message = "旧密码不能为空")
    private String password;

    @NotBlank(message = "新密码不能为空")
    @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d)\\S{8,20}$",
            message = "新密码为 8 至 20 位且至少含字母与数字，不含空格")
    private String newPassword;
}
