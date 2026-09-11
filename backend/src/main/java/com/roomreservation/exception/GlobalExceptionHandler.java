package com.roomreservation.exception;

import com.roomreservation.common.Constants;
import com.roomreservation.common.RiskBlockedException;
import com.roomreservation.common.Result;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;

@ControllerAdvice
/**
 * 全局异常处理：业务异常、风控异常与兜底异常统一转为 Result。
 */
public class GlobalExceptionHandler {

    /**
     * 业务异常统一返回 code 与提示
     */
    @ExceptionHandler(ServiceException.class)
    @ResponseBody
    public Result handle(ServiceException se) {
        return Result.error(se.getCode(), se.getMessage());
    }

    /**
     * 风控拦截，返回解除时间与次数
     */
    @ExceptionHandler(RiskBlockedException.class)
    @ResponseBody
    public Result handleRisk(RiskBlockedException e) {
        return Result.error(e.getCode(), e.getMessage(), e.getData());
    }

    /**
     * 兜底异常，联调期暴露消息便于定位
     */
    @ExceptionHandler(Exception.class)
    @ResponseBody
    public Result handleOther(Exception e) {
        return Result.error(Constants.CODE_500, "系统异常：" + e.getMessage());
    }

}
