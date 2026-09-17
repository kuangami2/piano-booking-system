package com.roomreservation.exception;

import com.roomreservation.common.Constants;
import com.roomreservation.common.Result;
import com.roomreservation.common.RiskBlockedException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 全局异常处理单元测试：业务异常、风控异常、字段校验与兜底异常的统一返回结构。
 */
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    @DisplayName("业务异常按原业务码与提示返回")
    void businessExceptionKeepsCodeAndMessage() {
        Result result = handler.handle(new ServiceException(Constants.CODE_409, "该时段已被预约"));
        assertThat(result.getCode()).isEqualTo(Constants.CODE_409);
        assertThat(result.getMsg()).isEqualTo("该时段已被预约");
        assertThat(result.getData()).isNull();
    }

    @Test
    @DisplayName("风控拦截携带解除时间与次数")
    void riskBlockedCarriesData() {
        Map<String, Object> data = Map.of("until", "2026-09-16 10:00:00", "nearCancelCount", 3);
        Result result = handler.handleRisk(new RiskBlockedException(Constants.CODE_409, "临近取消次数过多", data));
        assertThat(result.getCode()).isEqualTo(Constants.CODE_409);
        assertThat(result.getMsg()).isEqualTo("临近取消次数过多");
        assertThat(result.getData()).isEqualTo(data);
    }

    @Test
    @DisplayName("字段校验失败返回 400 与字段名")
    @SuppressWarnings("unchecked")
    void invalidArgumentReturnsField() {
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        BindingResult binding = mock(BindingResult.class);
        when(ex.getBindingResult()).thenReturn(binding);
        when(binding.getFieldError())
                .thenReturn(new FieldError("registerRequest", "password", "密码需包含字母与数字"));

        Result result = handler.handleInvalid(ex);

        assertThat(result.getCode()).isEqualTo(Constants.CODE_400);
        assertThat(result.getMsg()).isEqualTo("密码需包含字母与数字");
        assertThat((Map<String, Object>) result.getData()).containsEntry("field", "password");
    }

    @Test
    @DisplayName("字段校验无明细时回退默认提示")
    @SuppressWarnings("unchecked")
    void invalidArgumentWithoutFieldDetail() {
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        BindingResult binding = mock(BindingResult.class);
        when(ex.getBindingResult()).thenReturn(binding);
        when(binding.getFieldError()).thenReturn(null);

        Result result = handler.handleInvalid(ex);

        assertThat(result.getCode()).isEqualTo(Constants.CODE_400);
        assertThat(result.getMsg()).isEqualTo("参数校验失败");
        assertThat((Map<String, Object>) result.getData()).containsEntry("field", "");
    }

    @Test
    @DisplayName("请求不存在的接口返回 404 而不是系统异常")
    void noMappingReturns404() {
        Result result = handler.handleNotFound(new NoHandlerFoundException("PUT", "/api/bookings/1/cancel", null));
        assertThat(result.getCode()).isEqualTo(Constants.CODE_404);
        assertThat(result.getMsg()).contains("接口不存在");
    }

    @Test
    @DisplayName("兜底异常返回 500 与原始消息")
    void unexpectedExceptionReturns500() {
        Result result = handler.handleOther(new IllegalStateException("boom"));
        assertThat(result.getCode()).isEqualTo(Constants.CODE_500);
        assertThat(result.getMsg()).contains("boom");
    }
}
