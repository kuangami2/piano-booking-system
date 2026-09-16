package com.roomreservation.service;

import com.roomreservation.common.EnhanceProperties;
import com.roomreservation.dto.ActivityRankItem;
import com.roomreservation.entity.SysUser;
import com.roomreservation.mapper.SysUserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * 阶段 B 活跃度单元测试：开关与管理员过滤、权重计分、Redis ZSET 写入、降级排序与昵称脱敏。
 */
@ExtendWith(MockitoExtension.class)
class ActivityServiceTest {

    @Mock private IRuleConfigService ruleConfigService;
    @Mock private SysUserMapper sysUserMapper;
    @Mock private StringRedisTemplate redisTemplate;
    @Mock private ZSetOperations<String, String> zSetOperations;

    @InjectMocks private ActivityService service;

    private EnhanceProperties props;

    @BeforeEach
    void setUp() {
        props = new EnhanceProperties();
        ReflectionTestUtils.setField(service, "props", props);
        lenient().when(ruleConfigService.getInt(anyString(), anyInt()))
                .thenAnswer(invocation -> invocation.getArgument(1));
    }

    private SysUser user(int id, String name, String role) {
        SysUser user = new SysUser();
        user.setId(id);
        user.setName(name);
        user.setRole(role);
        return user;
    }

    @SuppressWarnings("unchecked")
    private List<ActivityRankItem> listOf(Map<String, Object> data) {
        return (List<ActivityRankItem>) data.get("list");
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> meOf(Map<String, Object> data) {
        return (Map<String, Object>) data.get("me");
    }

    @Test
    @DisplayName("活跃度开关关闭时不记录也不查用户")
    void skipsWhenDisabled() {
        props.setActivityEnabled(false);

        service.record(1, "booking");

        verifyNoInteractions(sysUserMapper, redisTemplate);
    }

    @Test
    @DisplayName("管理员行为不计分")
    void skipsAdmin() {
        when(sysUserMapper.selectById(1)).thenReturn(user(1, "管理员", "admin"));

        service.record(1, "booking");

        verifyNoInteractions(redisTemplate);
    }

    @Test
    @DisplayName("未知事件类型权重为零，不写缓存")
    void ignoresUnknownEvent() {
        when(sysUserMapper.selectById(1)).thenReturn(user(1, "张三", "user"));

        service.record(1, "unknown-event");

        verifyNoInteractions(redisTemplate);
    }

    @Test
    @DisplayName("Redis 可用时按周月两个 ZSET 累计分值")
    void writesBothPeriodsToRedis() {
        when(sysUserMapper.selectById(1)).thenReturn(user(1, "张三", "user"));
        when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);

        service.record(1, "booking");

        verify(zSetOperations, times(2)).incrementScore(anyString(), eq("1"), eq(5D));
        verify(redisTemplate, times(2)).expire(anyString(), any(Duration.class));
    }

    @Test
    @DisplayName("退约按负权重扣分")
    void cancelUsesNegativeWeight() {
        when(sysUserMapper.selectById(1)).thenReturn(user(1, "张三", "user"));
        when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);

        service.record(1, "cancel");

        verify(zSetOperations, times(2)).incrementScore(anyString(), eq("1"), eq(-3D));
    }

    @Test
    @DisplayName("Redis 异常时降级进程内计分，排行榜按分值排序并脱敏昵称")
    void fallsBackToLocalRanking() {
        when(sysUserMapper.selectById(anyInt()))
                .thenReturn(user(1, "张三", "user"), user(2, "李四", "user"));
        when(redisTemplate.opsForZSet()).thenThrow(new RuntimeException("redis down"));
        when(sysUserMapper.selectList(any())).thenReturn(List.of(user(1, "张三", "user"), user(2, "李四", "user")));

        service.record(1, "booking");
        service.record(2, "login");

        Map<String, Object> data = service.ranking("week", 1, 10, 2);
        List<ActivityRankItem> list = listOf(data);

        assertThat(list).hasSize(2);
        assertThat(list.get(0).userId()).isEqualTo(1);
        assertThat(list.get(0).name()).isEqualTo("张*");
        assertThat(list.get(0).score()).isEqualTo(5D);
        assertThat(list.get(1).name()).isEqualTo("李*");
        assertThat(meOf(data).get("rank")).isEqualTo(2);
        assertThat(meOf(data).get("score")).isEqualTo(1D);
        assertThat(data.get("updatedAt")).isNotNull();
    }

    @Test
    @DisplayName("同分按用户 ID 升序且分页只返回当页数据")
    void ranksWithStableOrderAndPaging() {
        when(sysUserMapper.selectById(anyInt())).thenReturn(user(1, "张三", "user"));
        when(redisTemplate.opsForZSet()).thenThrow(new RuntimeException("redis down"));
        when(sysUserMapper.selectList(any())).thenReturn(List.of(user(1, "张三", "user"), user(2, "李四", "user")));

        service.record(1, "booking");
        service.record(2, "booking");
        service.record(3, "login");

        Map<String, Object> firstPage = service.ranking("week", 1, 2, 3);
        assertThat(listOf(firstPage)).hasSize(2);
        assertThat(listOf(firstPage).get(0).userId()).isEqualTo(1);
        assertThat(listOf(firstPage).get(1).userId()).isEqualTo(2);
        assertThat(listOf(firstPage).get(0).rank()).isEqualTo(1);
        assertThat(meOf(firstPage).get("rank")).isEqualTo(3);

        Map<String, Object> secondPage = service.ranking("month", 2, 2, 1);
        assertThat(listOf(secondPage)).hasSize(1);
        assertThat(listOf(secondPage).get(0).rank()).isEqualTo(3);
    }

    @Test
    @DisplayName("无昵称用户显示匿名用户")
    void masksMissingName() {
        when(sysUserMapper.selectById(1)).thenReturn(user(1, "张三", "user"));
        when(redisTemplate.opsForZSet()).thenThrow(new RuntimeException("redis down"));
        when(sysUserMapper.selectList(any())).thenReturn(List.of());

        service.record(1, "booking");

        Map<String, Object> data = service.ranking("week", 1, 10, null);

        assertThat(listOf(data).get(0).name()).isEqualTo("匿名用户");
        assertThat(data.get("me")).isNull();
    }
}
