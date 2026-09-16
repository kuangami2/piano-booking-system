package com.roomreservation.controller;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.roomreservation.common.Constants;
import com.roomreservation.entity.Message;
import com.roomreservation.entity.SysUser;
import com.roomreservation.mapper.MessageMapper;
import com.roomreservation.service.ActivityService;
import com.roomreservation.utils.TokenUtils;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

/**
 * 消息接口 MockMvc 测试：一键已读作用范围、越权与不存在的消息处理。
 */
@WebMvcTest(controllers = MessageController.class)
class MessageControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @MockBean
    private MessageMapper messageMapper;
    @MockBean
    private ActivityService activityService;

    /** 纯单元测试没有 Spring 上下文，LambdaUpdateWrapper 的 set 需要实体元数据 */
    @BeforeAll
    static void initMybatisPlusTableInfo() {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        TableInfoHelper.initTableInfo(assistant, Message.class);
    }

    private SysUser currentUser() {
        SysUser user = new SysUser();
        user.setId(7);
        return user;
    }

    @Test
    @DisplayName("一键已读更新本人未读消息并记一次活跃度")
    void marksAllOwnMessagesRead() throws Exception {
        try (MockedStatic<TokenUtils> mocked = Mockito.mockStatic(TokenUtils.class)) {
            mocked.when(TokenUtils::getCurrentUser).thenReturn(currentUser());

            mockMvc.perform(put("/api/messages/read-all"))
                    .andExpect(jsonPath("$.code").value(Constants.CODE_200));

            verify(messageMapper).update(isNull(), any());
            verify(activityService).record(7, "messageRead");
        }
    }

    @Test
    @DisplayName("标记自己的单条消息已读")
    void marksSingleMessageRead() throws Exception {
        Message message = new Message();
        message.setId(3);
        message.setUserId(7);
        when(messageMapper.selectById(3)).thenReturn(message);

        try (MockedStatic<TokenUtils> mocked = Mockito.mockStatic(TokenUtils.class)) {
            mocked.when(TokenUtils::getCurrentUser).thenReturn(currentUser());

            mockMvc.perform(put("/api/messages/3/read"))
                    .andExpect(jsonPath("$.code").value(Constants.CODE_200));

            verify(messageMapper).update(isNull(), any());
            verify(activityService).record(7, "messageRead");
        }
    }

    @Test
    @DisplayName("标记他人消息返回 403 且不更新")
    void rejectsOtherUsersMessage() throws Exception {
        Message message = new Message();
        message.setId(3);
        message.setUserId(9);
        when(messageMapper.selectById(3)).thenReturn(message);

        try (MockedStatic<TokenUtils> mocked = Mockito.mockStatic(TokenUtils.class)) {
            mocked.when(TokenUtils::getCurrentUser).thenReturn(currentUser());

            mockMvc.perform(put("/api/messages/3/read"))
                    .andExpect(jsonPath("$.code").value(Constants.CODE_403));
        }
    }

    @Test
    @DisplayName("消息不存在返回 404")
    void rejectsMissingMessage() throws Exception {
        when(messageMapper.selectById(99)).thenReturn(null);

        try (MockedStatic<TokenUtils> mocked = Mockito.mockStatic(TokenUtils.class)) {
            mocked.when(TokenUtils::getCurrentUser).thenReturn(currentUser());

            mockMvc.perform(put("/api/messages/99/read"))
                    .andExpect(jsonPath("$.code").value(Constants.CODE_404));
        }
    }

    @Test
    @DisplayName("一键已读不写活跃度之外的任何单条更新")
    void readAllDoesNotTouchSingleRow() throws Exception {
        try (MockedStatic<TokenUtils> mocked = Mockito.mockStatic(TokenUtils.class)) {
            mocked.when(TokenUtils::getCurrentUser).thenReturn(currentUser());

            mockMvc.perform(put("/api/messages/read-all"));

            verify(messageMapper, never()).selectById(any());
        }
    }
}
