package com.roomreservation.service.admin;

import com.roomreservation.common.Constants;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.roomreservation.common.RuleKeys;
import com.roomreservation.entity.RuleConfig;
import com.roomreservation.exception.ServiceException;
import com.roomreservation.service.IRuleConfigService;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 规则参数保存单元测试：范围校验、非法类型、跨参数依赖与两阶段写入。
 */
@ExtendWith(MockitoExtension.class)
class AdminRuleServiceTest {

    @Mock private IRuleConfigService ruleConfigService;

    @InjectMocks private AdminRuleService service;

    /** 纯单元测试没有 Spring 上下文，LambdaUpdateWrapper 的 set 需要实体元数据 */
    @BeforeAll
    static void initMybatisPlusTableInfo() {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        TableInfoHelper.initTableInfo(assistant, RuleConfig.class);
    }

    private RuleConfig config(String key, String value) {
        RuleConfig c = new RuleConfig();
        c.setRuleKey(key);
        c.setRuleValue(value);
        return c;
    }

    private void givenCurrentRules() {
        lenient().when(ruleConfigService.list(any(Wrapper.class))).thenReturn(List.of(
                config(RuleKeys.BOOKING_MIN_UNIT, "30"),
                config(RuleKeys.BOOKING_MAX_DURATION, "240"),
                config(RuleKeys.BOOKING_ADVANCE_DAYS, "7"),
                config(RuleKeys.CREDIT_MAX, "100"),
                config(RuleKeys.CREDIT_LOW, "60"),
                config(RuleKeys.RISK_NEAR_CANCEL_WINDOW, "120")));
        lenient().when(ruleConfigService.count(any())).thenReturn(1L);
    }

    @Test
    @DisplayName("时间粒度设为 0 被拒绝，且不写库")
    void rejectsZeroMinUnit() {
        givenCurrentRules();

        assertThatThrownBy(() -> service.batchUpdate(List.of(Map.of("key", RuleKeys.BOOKING_MIN_UNIT, "value", "0"))))
                .isInstanceOf(ServiceException.class)
                .hasFieldOrPropertyWithValue("code", Constants.CODE_400)
                .hasMessageContaining("应在");

        verify(ruleConfigService, never()).update(any(Wrapper.class));
        verify(ruleConfigService, never()).refreshCache();
    }

    @Test
    @DisplayName("负数与超大值被拒绝")
    void rejectsNegativeAndHugeValues() {
        givenCurrentRules();

        assertThatThrownBy(() -> service.batchUpdate(List.of(Map.of("key", RuleKeys.BOOKING_WEEKLY_LIMIT, "value", "-1"))))
                .isInstanceOf(ServiceException.class);
        assertThatThrownBy(() -> service.batchUpdate(List.of(Map.of("key", RuleKeys.BOOKING_ADVANCE_DAYS, "value", "999"))))
                .isInstanceOf(ServiceException.class);
    }

    @Test
    @DisplayName("非整数值被拒绝")
    void rejectsNonNumericValue() {
        givenCurrentRules();

        assertThatThrownBy(() -> service.batchUpdate(List.of(Map.of("key", RuleKeys.BOOKING_MIN_UNIT, "value", "abc"))))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("整数");
    }

    @Test
    @DisplayName("跨参数依赖冲突被拒绝：暂停阈值高于信用上限")
    void rejectsDependencyConflict() {
        givenCurrentRules();

        assertThatThrownBy(() -> service.batchUpdate(List.of(Map.of("key", RuleKeys.CREDIT_LOW, "value", "200"))))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("暂停阈值");
        verify(ruleConfigService, never()).update(any(Wrapper.class));
    }

    @Test
    @DisplayName("合法参数保存成功并刷新缓存")
    void savesValidRuleAndRefreshesCache() {
        givenCurrentRules();
        when(ruleConfigService.list(any(Wrapper.class))).thenReturn(List.of(config(RuleKeys.BOOKING_MIN_UNIT, "60")));

        List<RuleConfig> result = service.batchUpdate(List.of(Map.of("key", RuleKeys.BOOKING_MIN_UNIT, "value", "60")));

        verify(ruleConfigService).update(any(Wrapper.class));
        verify(ruleConfigService).refreshCache();
        assertThat(result).isNotEmpty();
    }

    @Test
    @DisplayName("库中 minUnit 为脏值 0 时保存其他参数不抛除零异常")
    void toleratesDirtyStoredMinUnit() {
        when(ruleConfigService.list(any(Wrapper.class))).thenReturn(List.of(
                config(RuleKeys.BOOKING_MIN_UNIT, "0"),
                config(RuleKeys.BOOKING_MAX_DURATION, "240"),
                config(RuleKeys.CREDIT_MAX, "100"),
                config(RuleKeys.CREDIT_LOW, "60")));
        when(ruleConfigService.count(any())).thenReturn(1L);

        List<RuleConfig> result = service.batchUpdate(List.of(
                Map.of("key", RuleKeys.BOOKING_ADVANCE_DAYS, "value", "10")));

        verify(ruleConfigService).refreshCache();
        assertThat(result).isNotEmpty();
    }

    @Test
    @DisplayName("批次中任一条非法则整批不写")
    void rejectsWholeBatchWhenAnyInvalid() {
        givenCurrentRules();

        assertThatThrownBy(() -> service.batchUpdate(List.of(
                Map.of("key", RuleKeys.BOOKING_MIN_UNIT, "value", "60"),
                Map.of("key", RuleKeys.BOOKING_MAX_DURATION, "value", "0"))))
                .isInstanceOf(ServiceException.class);

        verify(ruleConfigService, never()).update(any(Wrapper.class));
    }
}
