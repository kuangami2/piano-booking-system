package com.roomreservation.config.interceptor;

import com.roomreservation.entity.SysUser;
import com.roomreservation.exception.ServiceException;
import com.roomreservation.service.ISysUserService;
import com.roomreservation.utils.TokenUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.method.HandlerMethod;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/**
 * 鉴权拦截器单元测试：公开接口放行、token 缺失与非法拦截、验签失败、角色校验。
 */
class JwtInterceptorTest {

    private final JwtInterceptor interceptor = new JwtInterceptor();

    @Mock
    private ISysUserService sysUserService;
    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;

    /** 用于构造 HandlerMethod 的样例控制器 */
    static class Sample {
        @AuthAccess
        public void open() {
        }

        @RequireRole("admin")
        public void adminOnly() {
        }

        public void normal() {
        }
    }

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        ReflectionTestUtils.setField(interceptor, "sysUserService", sysUserService);
    }

    private HandlerMethod handler(String method) throws NoSuchMethodException {
        return new HandlerMethod(new Sample(), Sample.class.getMethod(method));
    }

    @Test
    @DisplayName("公开接口直接放行，不校验 token")
    void allowsPublicEndpoint() throws Exception {
        assertThat(interceptor.preHandle(request, response, handler("open"))).isTrue();
    }

    @Test
    @DisplayName("非控制器方法直接放行")
    void allowsNonHandler() {
        assertThat(interceptor.preHandle(request, response, new Object())).isTrue();
    }

    @Test
    @DisplayName("缺少 token 返回 401")
    void rejectsMissingToken() throws Exception {
        when(request.getHeader("token")).thenReturn(null);
        when(request.getParameter("token")).thenReturn(null);

        assertThatThrownBy(() -> interceptor.preHandle(request, response, handler("normal")))
                .isInstanceOf(ServiceException.class)
                .hasFieldOrPropertyWithValue("code", "401");
    }

    @Test
    @DisplayName("token 载荷非法返回 401")
    void rejectsMalformedToken() throws Exception {
        when(request.getHeader("token")).thenReturn("not-a-jwt");

        assertThatThrownBy(() -> interceptor.preHandle(request, response, handler("normal")))
                .isInstanceOf(ServiceException.class)
                .hasFieldOrPropertyWithValue("code", "401");
    }

    @Test
    @DisplayName("用户不存在返回 401")
    void rejectsUnknownUser() throws Exception {
        String token = TokenUtils.createToken(9, "user", "hash");
        when(request.getHeader("token")).thenReturn(token);
        when(sysUserService.getById(9)).thenReturn(null);

        assertThatThrownBy(() -> interceptor.preHandle(request, response, handler("normal")))
                .isInstanceOf(ServiceException.class)
                .hasFieldOrPropertyWithValue("code", "401");
    }

    @Test
    @DisplayName("密码已变更导致验签失败返回 401")
    void rejectsTokenSignedByOldPassword() throws Exception {
        String token = TokenUtils.createToken(9, "user", "old-hash");
        SysUser user = new SysUser();
        user.setId(9);
        user.setPassword("new-hash");
        when(request.getHeader("token")).thenReturn(token);
        when(sysUserService.getById(9)).thenReturn(user);

        assertThatThrownBy(() -> interceptor.preHandle(request, response, handler("normal")))
                .isInstanceOf(ServiceException.class)
                .hasFieldOrPropertyWithValue("code", "401");
    }

    @Test
    @DisplayName("角色不匹配返回 403")
    void rejectsWrongRole() throws Exception {
        String token = TokenUtils.createToken(9, "user", "hash");
        SysUser user = new SysUser();
        user.setId(9);
        user.setPassword("hash");
        when(request.getHeader("token")).thenReturn(token);
        when(sysUserService.getById(9)).thenReturn(user);

        assertThatThrownBy(() -> interceptor.preHandle(request, response, handler("adminOnly")))
                .isInstanceOf(ServiceException.class)
                .hasFieldOrPropertyWithValue("code", "403");
    }

    @Test
    @DisplayName("token 合法且角色匹配时放行")
    void allowsValidToken() throws Exception {
        String token = TokenUtils.createToken(9, "admin", "hash");
        SysUser user = new SysUser();
        user.setId(9);
        user.setPassword("hash");
        when(request.getHeader("token")).thenReturn(token);
        when(sysUserService.getById(9)).thenReturn(user);

        assertThat(interceptor.preHandle(request, response, handler("adminOnly"))).isTrue();
    }
}
