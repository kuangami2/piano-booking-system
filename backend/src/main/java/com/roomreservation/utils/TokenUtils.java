package com.roomreservation.utils;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.roomreservation.entity.SysUser;
import com.roomreservation.service.ISysUserService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Date;

@Component
public class TokenUtils {

    private static ISysUserService staticUserService;

    @Resource
    private ISysUserService userService;

    @PostConstruct
    public void initService() {
        staticUserService = userService;
    }

    /**
     * 生成 token，载荷为 用户id-角色，密钥为用户密码，2 小时过期
     */
    public static String createToken(Integer userId, String role, String sign) {
        return JWT.create()
                .withAudience(userId + "-" + role)
                .withExpiresAt(DateUtil.offsetHour(new Date(), 2))
                .sign(Algorithm.HMAC256(sign));
    }

    /**
     * 获取当前登录用户，未登录返回 null
     */
    public static SysUser getCurrentUser() {
        try {
            HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
            String token = request.getHeader("token");
            if (StrUtil.isNotEmpty(token)) {
                String info = JWT.decode(token).getAudience().get(0);
                Integer userId = Integer.valueOf(info.split("-")[0]);
                return staticUserService.getById(userId);
            }
        } catch (Exception e) {
            return null;
        }
        return null;
    }
}
