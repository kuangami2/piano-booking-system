package com.roomreservation.common;

public interface Constants {

    String CODE_200 = "200"; // 请求成功
    String CODE_400 = "400"; // 参数错误
    String CODE_401 = "401"; // 未登录或 token 失效
    String CODE_403 = "403"; // 无权限
    String CODE_404 = "404"; // 资源不存在
    String CODE_409 = "409"; // 业务冲突
    String CODE_429 = "429"; // 请求过于频繁，触发限流
    String CODE_500 = "500"; // 系统错误

}
