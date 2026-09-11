package com.roomreservation.config.interceptor;

import cn.hutool.core.util.StrUtil;
import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.roomreservation.common.Constants;
import com.roomreservation.entity.SysUser;
import com.roomreservation.exception.ServiceException;
import com.roomreservation.service.ISysUserService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 鉴权拦截器：解析 token、验签、校验用户与角色注解，公开接口放行。
 */
public class JwtInterceptor implements HandlerInterceptor {

    @Resource
    private ISysUserService sysUserService;

    @Override
    /**
     * 请求前置拦截：公开接口放行，其余解析 token 载荷取得用户与角色，
     * 用该用户密码的 BCrypt hash 验签，校验用户存在与角色注解，失败抛 401 或 403。
     */
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // 非控制器方法直接放行
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }
        // 公开接口直接放行
        if (handlerMethod.getMethodAnnotation(AuthAccess.class) != null) {
            return true;
        }
        String token = request.getHeader("token");
        if (StrUtil.isBlank(token)) {
            token = request.getParameter("token");
        }
        if (StrUtil.isBlank(token)) {
            throw new ServiceException(Constants.CODE_401, "无 token，请重新登录");
        }
        // 解析 token 载荷，格式 用户id-角色
        Integer userId;
        String role;
        try {
            String info = JWT.decode(token).getAudience().get(0);
            userId = Integer.valueOf(info.split("-")[0]);
            role = info.split("-")[1];
        } catch (RuntimeException e) {
            throw new ServiceException(Constants.CODE_401, "token 验证失败，请重新登录");
        }
        SysUser user = sysUserService.getById(userId);
        if (user == null) {
            throw new ServiceException(Constants.CODE_401, "用户不存在，请重新登录");
        }
        // 以用户密码加签验证 token
        JWTVerifier jwtVerifier = JWT.require(Algorithm.HMAC256(user.getPassword())).build();
        try {
            jwtVerifier.verify(token);
        } catch (JWTVerificationException e) {
            throw new ServiceException(Constants.CODE_401, "token 验证失败，请重新登录");
        }
        // 角色校验
        RequireRole requireRole = handlerMethod.getMethodAnnotation(RequireRole.class);
        if (requireRole != null && !requireRole.value().equals(role)) {
            throw new ServiceException(Constants.CODE_403, "无权限访问");
        }
        return true;
    }
}
