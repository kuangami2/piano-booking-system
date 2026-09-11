package com.roomreservation.exception;

import lombok.Getter;

/**
 * 业务异常，携带响应码
 */
@Getter
/**
 * 业务异常：携带业务码与提示信息。
 */
public class ServiceException extends RuntimeException {

    private final String code;

    public ServiceException(String code, String msg) {
        super(msg);
        this.code = code;
    }

}
