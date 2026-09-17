package com.huicang.wise.api.support;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;

/**
 * 「已认证 + 已授权」的 Web 切片测试基类。
 *
 * <p>用途：多数安全/接口测试关注的是**入参处理与响应结构**（SQL 注入、XSS、越权防护的输入侧），
 * 而非鉴权链路本身。但 {@code SecurityConfig} + {@code JwtAuthenticationFilter} 会拦截未认证请求并返回 401/403，
 * 使这些用例在真正验证自身逻辑之前就被安全层挡下。
 *
 * <p>本类在 {@link AbstractWebMvcSliceTest} 的基础上，于每个用例执行前把 JWT 校验与权限校验打桩为「通过」，
 * 让请求能抵达控制器；需要**验证鉴权失败行为**的用例可在自身用例内重新打桩覆盖。
 *
 * <p>注意：本类不做 {@code addFilters=false}，Spring Security 过滤器链仍然生效，
 * 因此 CSRF 等安全机制依旧可被验证。
 *
 * @author WiseDepot
 * @version 1.0
 * @since 2026-02-27
 */
public abstract class AbstractAuthenticatedSliceTest extends AbstractWebMvcSliceTest {

    /**
     * 在用例执行前把 JWT 与权限校验打桩为通过。
     */
    @BeforeEach
    void stubAuthenticationAndPermission() {
        when(jwtTokenProvider.validateToken(anyString())).thenReturn(true);
        when(jwtTokenProvider.getUsernameFromToken(anyString())).thenReturn("admin");
        when(jwtTokenProvider.getUserIdFromToken(anyString())).thenReturn(1L);
        when(jwtTokenProvider.isTokenExpired(anyString())).thenReturn(false);
        when(jwtTokenProvider.isTokenType(anyString(), anyString())).thenReturn(true);

        when(permissionService.hasPermission(any(), anyString())).thenReturn(true);
    }
}
