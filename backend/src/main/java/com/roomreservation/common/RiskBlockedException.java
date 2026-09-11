package com.roomreservation.common;

import lombok.Getter;

/**
 * 风控拦截异常，附带 data 便于前端展示解除时间与原因
 */
@Getter
/**
 * 风控拦截异常：携带业务码与 data，供前端展示解除时间与原因。
 */
public class RiskBlockedException extends RuntimeException {

    private final String code;
    private final Object data;

    public RiskBlockedException(String code, String msg, Object data) {
        super(msg);
        this.code = code;
        this.data = data;
    }
}
