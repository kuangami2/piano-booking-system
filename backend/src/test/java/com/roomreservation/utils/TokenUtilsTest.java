package com.roomreservation.utils;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Token 工具单元测试：载荷格式、密码 hash 验签与两小时过期。
 */
class TokenUtilsTest {

    @Test
    @DisplayName("载荷为用户id-角色，可用密码 hash 验签")
    void createsTokenWithAudienceAndPasswordSignature() {
        String token = TokenUtils.createToken(7, "user", "bcrypt-hash");
        DecodedJWT decoded = JWT.require(Algorithm.HMAC256("bcrypt-hash")).build().verify(token);
        assertThat(decoded.getAudience()).containsExactly("7-user");
    }

    @Test
    @DisplayName("管理员角色同样以 用户id-角色 形式入载荷")
    void keepsAdminRoleInAudience() {
        String token = TokenUtils.createToken(1, "admin", "hash");
        DecodedJWT decoded = JWT.decode(token);
        assertThat(decoded.getAudience()).containsExactly("1-admin");
    }

    @Test
    @DisplayName("改密后旧 token 验签失败，无需黑名单即失效")
    void invalidAfterPasswordChange() {
        String token = TokenUtils.createToken(7, "user", "old-hash");
        assertThatThrownBy(() -> JWT.require(Algorithm.HMAC256("new-hash")).build().verify(token))
                .isInstanceOf(JWTVerificationException.class);
    }

    @Test
    @DisplayName("过期时间为两小时")
    void expiresInTwoHours() {
        String token = TokenUtils.createToken(7, "user", "hash");
        DecodedJWT decoded = JWT.decode(token);
        long minutes = ChronoUnit.MINUTES.between(Instant.now(), decoded.getExpiresAt().toInstant());
        assertThat(minutes).isBetween(118L, 120L);
    }
}
