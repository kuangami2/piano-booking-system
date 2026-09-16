package com.roomreservation.controller;

import com.roomreservation.controller.admin.AdminEventsController;
import com.roomreservation.service.RateLimitService;
import com.roomreservation.service.admin.AdminEventService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 管理端事件概览 MockMvc 测试：正常模式返回计数，降级模式返回 null 字段不报错。
 */
@WebMvcTest(controllers = AdminEventsController.class)
class AdminEventsControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @MockBean
    private AdminEventService adminEventService;
    @MockBean
    private RateLimitService rateLimitService;

    @Test
    @DisplayName("异步模式返回 outbox 计数与事件统计")
    void returnsAsyncSummary() throws Exception {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("mqEnabled", true);
        data.put("mode", "async");
        data.put("brokerReachable", true);
        data.put("outbox", Map.of("pending", 0, "sent", 5));
        data.put("dlq", Map.of("depth", 0, "redelivered", 1));
        data.put("events", new ArrayList<>());
        data.put("lastEventAt", "2026-09-17 01:00:00");
        when(adminEventService.summary()).thenReturn(data);

        mockMvc.perform(get("/api/admin/events/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.data.mode").value("async"))
                .andExpect(jsonPath("$.data.outbox.sent").value(5))
                .andExpect(jsonPath("$.data.dlq.redelivered").value(1));
    }

    @Test
    @DisplayName("降级模式 outbox 与 dlq 为 null 且不报错")
    void returnsDegradedSummary() throws Exception {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("mqEnabled", true);
        data.put("mode", "degraded");
        data.put("brokerReachable", false);
        data.put("outbox", null);
        data.put("dlq", null);
        data.put("events", new ArrayList<>());
        data.put("lastEventAt", null);
        when(adminEventService.summary()).thenReturn(data);

        mockMvc.perform(get("/api/admin/events/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.mode").value("degraded"))
                .andExpect(jsonPath("$.data.brokerReachable").value(false))
                .andExpect(jsonPath("$.data.outbox").doesNotExist());
    }

    @Test
    @DisplayName("概览接口按每分钟 30 次限流")
    void appliesRateLimit() throws Exception {
        when(adminEventService.summary()).thenReturn(Map.of("mode", "async"));

        mockMvc.perform(get("/api/admin/events/summary")).andExpect(status().isOk());

        verify(rateLimitService).check(any(), any(), org.mockito.ArgumentMatchers.eq(30));
    }
}
