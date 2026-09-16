package com.roomreservation.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.roomreservation.entity.SysUser;
import com.roomreservation.service.ActivityService;
import com.roomreservation.service.ISysUserService;
import com.roomreservation.service.RateLimitService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 鉴权接口 MockMvc 测试：字段校验失败返回 400 与字段名、注册成功、找回密码返回令牌。
 */
@WebMvcTest(controllers = AuthController.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @MockBean
    private ISysUserService sysUserService;
    @MockBean
    private ActivityService activityService;
    @MockBean
    private RateLimitService rateLimitService;

    private String json(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }

    @Test
    @DisplayName("注册字段非法返回 400 且带字段名")
    void rejectsInvalidRegisterFields() throws Exception {
        String body = json(Map.of("username", "ab", "password", "123",
                "name", "A1", "studentNo", "123", "email", "bad"));

        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("400"))
                .andExpect(jsonPath("$.data.field").isNotEmpty());
    }

    @Test
    @DisplayName("注册字段合法时调用服务并返回成功")
    void registersValidUser() throws Exception {
        String body = json(Map.of("username", "tester01", "password", "pass1234",
                "name", "Tester", "studentNo", "2023000001", "email", "tester01@test.com"));

        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"));
        verify(sysUserService).register(any());
    }

    @Test
    @DisplayName("登录成功签发 token 且不回传密码")
    void loginReturnsToken() throws Exception {
        SysUser user = new SysUser();
        user.setId(1);
        user.setRole("user");
        user.setPassword("bcrypt-hash");
        when(sysUserService.login("tester01", "pass1234")).thenReturn(user);

        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("username", "tester01", "password", "pass1234"))))
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.data.token").isNotEmpty())
                .andExpect(jsonPath("$.data.user.password").doesNotExist());
    }

    @Test
    @DisplayName("找回密码返回 sent 标记与本地联调令牌")
    void forgotReturnsTokenWhenMailDisabled() throws Exception {
        when(sysUserService.forgotPassword("tester01@test.com")).thenReturn("reset-token");

        mockMvc.perform(post("/api/auth/forgot").contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", "tester01@test.com"))))
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.data.sent").value(true))
                .andExpect(jsonPath("$.data.expireMinutes").value(30))
                .andExpect(jsonPath("$.data.token").value("reset-token"));
    }

    @Test
    @DisplayName("邮箱不存在时不暴露注册状态")
    void forgotHidesUnknownEmail() throws Exception {
        when(sysUserService.forgotPassword("nobody@test.com")).thenReturn(null);

        mockMvc.perform(post("/api/auth/forgot").contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", "nobody@test.com"))))
                .andExpect(jsonPath("$.data.sent").value(false))
                .andExpect(jsonPath("$.data.token").doesNotExist());
    }
}
