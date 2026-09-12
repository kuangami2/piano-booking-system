package com.roomreservation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * 凭令牌重置密码，新密码与注册同口径
 */
@Data
public class ResetPasswordRequest {

    @NotBlank(message = "重置令牌不能为空")
    private String token;

    @NotBlank(message = "新密码不能为空")
    @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d)\\S{8,20}$",
            message = "新密码为 8 至 20 位且至少含字母与数字，不含空格")
    private String newPassword;
}
