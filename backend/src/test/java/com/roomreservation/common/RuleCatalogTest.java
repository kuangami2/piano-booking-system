package com.roomreservation.common;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 规则参数元数据单元测试：区间钳制、范围校验与跨参数依赖校验。
 */
class RuleCatalogTest {

    @Test
    @DisplayName("18 个规则键都有元数据且默认值在合法区间内")
    void everyRuleHasMetaWithinRange() {
        assertThat(RuleCatalog.all()).hasSize(18);
        for (RuleCatalog.Meta meta : RuleCatalog.all()) {
            assertThat(meta.min()).isLessThanOrEqualTo(meta.defaultValue());
            assertThat(meta.defaultValue()).isLessThanOrEqualTo(meta.max());
            assertThat(meta.min()).isLessThan(meta.max());
        }
    }

    @Test
    @DisplayName("读取钳制：低于下限取下限，高于上限取上限，正常值不变")
    void clampsOutOfRangeValues() {
        assertThat(RuleCatalog.clamp(RuleKeys.BOOKING_MIN_UNIT, 0)).isEqualTo(5);
        assertThat(RuleCatalog.clamp(RuleKeys.BOOKING_MIN_UNIT, -30)).isEqualTo(5);
        assertThat(RuleCatalog.clamp(RuleKeys.BOOKING_MIN_UNIT, 99999)).isEqualTo(240);
        assertThat(RuleCatalog.clamp(RuleKeys.BOOKING_MIN_UNIT, 30)).isEqualTo(30);
        assertThat(RuleCatalog.clamp("unknown.key", 7)).isEqualTo(7);
    }

    @Test
    @DisplayName("范围校验：越界与未知参数返回错误，合法返回空")
    void validatesRange() {
        assertThat(RuleCatalog.validateRange(RuleKeys.BOOKING_MIN_UNIT, 30)).isNull();
        assertThat(RuleCatalog.validateRange(RuleKeys.BOOKING_MIN_UNIT, 0)).contains("应在");
        assertThat(RuleCatalog.validateRange(RuleKeys.BOOKING_MIN_UNIT, 10000)).contains("应在");
        assertThat(RuleCatalog.validateRange("unknown.key", 1)).contains("未知参数");
    }

    @Test
    @DisplayName("依赖校验：暂停阈值高于信用上限被拒")
    void rejectsThresholdAboveMaxCredit() {
        Map<String, Integer> values = new HashMap<>();
        values.put(RuleKeys.CREDIT_MAX, 100);
        values.put(RuleKeys.CREDIT_LOW, 120);

        assertThat(RuleCatalog.validateDependencies(values)).contains("暂停阈值");
    }

    @Test
    @DisplayName("依赖校验：最大时长小于粒度或不成整数倍被拒")
    void rejectsDurationAgainstUnit() {
        Map<String, Integer> values = new HashMap<>();
        values.put(RuleKeys.BOOKING_MIN_UNIT, 30);
        values.put(RuleKeys.BOOKING_MAX_DURATION, 20);
        assertThat(RuleCatalog.validateDependencies(values)).contains("不能小于");

        values.put(RuleKeys.BOOKING_MAX_DURATION, 100);
        assertThat(RuleCatalog.validateDependencies(values)).contains("整数倍");
    }

    @Test
    @DisplayName("依赖校验：临近取消窗口超过可提前预约总时长被拒")
    void rejectsWindowBeyondAdvanceDays() {
        Map<String, Integer> values = new HashMap<>();
        values.put(RuleKeys.BOOKING_ADVANCE_DAYS, 1);
        values.put(RuleKeys.RISK_NEAR_CANCEL_WINDOW, 2000);

        assertThat(RuleCatalog.validateDependencies(values)).contains("临近取消窗口");
    }

    @Test
    @DisplayName("依赖校验：库中 minUnit 为脏值 0 时报范围错误而非抛除零异常")
    void rejectsDirtyMinUnitWithoutArithmeticError() {
        Map<String, Integer> values = new HashMap<>();
        values.put(RuleKeys.BOOKING_MIN_UNIT, 0);
        values.put(RuleKeys.BOOKING_MAX_DURATION, 240);

        assertThat(RuleCatalog.validateDependencies(values)).contains("应在 5 到 240 之间");
    }

    @Test
    @DisplayName("依赖校验：合法的默认组合通过")
    void acceptsDefaultCombination() {
        Map<String, Integer> values = new HashMap<>();
        values.put(RuleKeys.CREDIT_MAX, 100);
        values.put(RuleKeys.CREDIT_LOW, 60);
        values.put(RuleKeys.CREDIT_INITIAL, 100);
        values.put(RuleKeys.BOOKING_MIN_UNIT, 30);
        values.put(RuleKeys.BOOKING_MAX_DURATION, 240);
        values.put(RuleKeys.BOOKING_ADVANCE_DAYS, 7);
        values.put(RuleKeys.RISK_NEAR_CANCEL_WINDOW, 1440);

        assertThat(RuleCatalog.validateDependencies(values)).isNull();
    }
}
