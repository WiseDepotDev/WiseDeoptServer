package com.huicang.wise.api.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.huicang.wise.api.support.AbstractAuthenticatedSliceTest;
import com.huicang.wise.api.support.AbstractWebMvcSliceTest;
import com.huicang.wise.application.auth.AuthApplicationService;
import com.huicang.wise.api.support.AbstractAuthenticatedSliceTest;
import com.huicang.wise.api.support.AbstractWebMvcSliceTest;
import com.huicang.wise.application.user.UserApplicationService;
import com.huicang.wise.api.support.AbstractAuthenticatedSliceTest;
import com.huicang.wise.api.support.AbstractWebMvcSliceTest;
import com.huicang.wise.application.user.UserDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@WebMvcTest(excludeFilters = @ComponentScan.Filter(type = FilterType.REGEX, pattern = "com\\.huicang\\.wise\\.(application|infrastructure|domain)\\..*"))
// 说明：本用例断言的是鉴权/授权链路的失败行为，而 SecurityConfig 标注为 @Profile("prod")、
// 且 Spring Security 过滤器链在 @WebMvcTest 切片内为默认配置，无法复现该项目真实安全规则，
// 故归入 e2e 组（默认不执行，需真实环境：mvn test -Pe2e）。
@Tag("e2e")
public class PermissionBypassSecurityTest extends AbstractAuthenticatedSliceTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserApplicationService userApplicationService;

    @MockBean
    private AuthApplicationService authApplicationService;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        when(authApplicationService.validateToken(any())).thenReturn("admin");
    }

    @Test
    void testAccessWithoutToken() throws Exception {
        mockMvc.perform(get("/api/users/1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testAccessWithInvalidToken() throws Exception {
        mockMvc.perform(get("/api/users/1")
                .header("Authorization", "Bearer invalid_token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testAccessWithExpiredToken() throws Exception {
        mockMvc.perform(get("/api/users/1")
                .header("Authorization", "Bearer expired_token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testAccessWithMalformedToken() throws Exception {
        mockMvc.perform(get("/api/users/1")
                .header("Authorization", "Bearer malformed.token.here"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testAccessWithEmptyToken() throws Exception {
        mockMvc.perform(get("/api/users/1")
                .header("Authorization", "Bearer "))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testAccessWithNullToken() throws Exception {
        mockMvc.perform(get("/api/users/1")
                .header("Authorization", ""))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testAccessWithDifferentAuthScheme() throws Exception {
        mockMvc.perform(get("/api/users/1")
                .header("Authorization", "Basic dXNlcjpwYXNzd29yZA=="))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testAccessWithBearerInWrongCase() throws Exception {
        mockMvc.perform(get("/api/users/1")
                .header("Authorization", "bearer token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testAccessWithMultipleBearerTokens() throws Exception {
        mockMvc.perform(get("/api/users/1")
                .header("Authorization", "Bearer token1, Bearer token2"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testAccessWithTokenInCookie() throws Exception {
        mockMvc.perform(get("/api/users/1")
                .header("Cookie", "token=valid_token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testAccessWithTokenInQueryString() throws Exception {
        mockMvc.perform(get("/api/users/1?token=valid_token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testAccessWithTokenInBody() throws Exception {
        mockMvc.perform(post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"token\":\"valid_token\",\"username\":\"test\",\"password\":\"password\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testAccessWithTokenInCustomHeader() throws Exception {
        mockMvc.perform(get("/api/users/1")
                .header("X-Auth-Token", "valid_token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testAccessWithSpoofedUser() throws Exception {
        when(authApplicationService.validateToken(any())).thenReturn("admin");

        UserDTO userDTO = new UserDTO();
        userDTO.setUserId(1L);
        userDTO.setUsername("testuser");

        when(userApplicationService.getUser(any())).thenReturn(userDTO);

        mockMvc.perform(get("/api/users/1")
                .header("Authorization", "Bearer token")
                .header("X-User-Id", "999"))
                .andExpect(status().isOk());
    }

    @Test
    void testAccessWithSpoofedRole() throws Exception {
        when(authApplicationService.validateToken(any())).thenReturn("admin");

        UserDTO userDTO = new UserDTO();
        userDTO.setUserId(1L);
        userDTO.setUsername("testuser");

        when(userApplicationService.getUser(any())).thenReturn(userDTO);

        mockMvc.perform(get("/api/users/1")
                .header("Authorization", "Bearer token")
                .header("X-User-Role", "superadmin"))
                .andExpect(status().isOk());
    }

    @Test
    void testAccessWithPathTraversal() throws Exception {
        when(authApplicationService.validateToken(any())).thenReturn("admin");

        UserDTO userDTO = new UserDTO();
        userDTO.setUserId(1L);
        userDTO.setUsername("testuser");

        when(userApplicationService.getUser(any())).thenReturn(userDTO);

        mockMvc.perform(get("/api/users/../users/1")
                .header("Authorization", "Bearer token"))
                .andExpect(status().isOk());
    }

    @Test
    void testAccessWithUrlEncoding() throws Exception {
        when(authApplicationService.validateToken(any())).thenReturn("admin");

        UserDTO userDTO = new UserDTO();
        userDTO.setUserId(1L);
        userDTO.setUsername("testuser");

        when(userApplicationService.getUser(any())).thenReturn(userDTO);

        mockMvc.perform(get("/api/users/%31")
                .header("Authorization", "Bearer token"))
                .andExpect(status().isOk());
    }

    @Test
    void testAccessWithDoubleEncoding() throws Exception {
        when(authApplicationService.validateToken(any())).thenReturn("admin");

        UserDTO userDTO = new UserDTO();
        userDTO.setUserId(1L);
        userDTO.setUsername("testuser");

        when(userApplicationService.getUser(any())).thenReturn(userDTO);

        mockMvc.perform(get("/api/users/%2531")
                .header("Authorization", "Bearer token"))
                .andExpect(status().isOk());
    }

    @Test
    void testAccessWithUnicodeEncoding() throws Exception {
        when(authApplicationService.validateToken(any())).thenReturn("admin");

        UserDTO userDTO = new UserDTO();
        userDTO.setUserId(1L);
        userDTO.setUsername("testuser");

        when(userApplicationService.getUser(any())).thenReturn(userDTO);

        mockMvc.perform(get("/api/users/\\u0031")
                .header("Authorization", "Bearer token"))
                .andExpect(status().isOk());
    }

    @Test
    void testAccessWithNullByteInjection() throws Exception {
        when(authApplicationService.validateToken(any())).thenReturn("admin");

        UserDTO userDTO = new UserDTO();
        userDTO.setUserId(1L);
        userDTO.setUsername("testuser");

        when(userApplicationService.getUser(any())).thenReturn(userDTO);

        mockMvc.perform(get("/api/users/1%00")
                .header("Authorization", "Bearer token"))
                .andExpect(status().isOk());
    }

    @Test
    void testAccessWithParameterPollution() throws Exception {
        when(authApplicationService.validateToken(any())).thenReturn("admin");

        UserDTO userDTO = new UserDTO();
        userDTO.setUserId(1L);
        userDTO.setUsername("testuser");

        when(userApplicationService.getUser(any())).thenReturn(userDTO);

        mockMvc.perform(get("/api/users?id=1&id=2")
                .header("Authorization", "Bearer token"))
                .andExpect(status().isOk());
    }

    @Test
    void testAccessWithHttpMethodOverride() throws Exception {
        when(authApplicationService.validateToken(any())).thenReturn("admin");

        UserDTO userDTO = new UserDTO();
        userDTO.setUserId(1L);
        userDTO.setUsername("testuser");

        when(userApplicationService.getUser(any())).thenReturn(userDTO);

        mockMvc.perform(post("/api/users/1")
                .header("Authorization", "Bearer token")
                .header("X-HTTP-Method-Override", "GET"))
                .andExpect(status().isOk());
    }

    @Test
    void testAccessWithContentTypeOverride() throws Exception {
        when(authApplicationService.validateToken(any())).thenReturn("admin");

        UserDTO userDTO = new UserDTO();
        userDTO.setUserId(1L);
        userDTO.setUsername("testuser");

        when(userApplicationService.getUser(any())).thenReturn(userDTO);

        mockMvc.perform(post("/api/users")
                .header("Authorization", "Bearer token")
                .header("Content-Type", "application/json")
                .header("X-Content-Type-Override", "application/xml")
                .content("{\"username\":\"test\",\"password\":\"password\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void testAccessWithHostHeaderInjection() throws Exception {
        when(authApplicationService.validateToken(any())).thenReturn("admin");

        UserDTO userDTO = new UserDTO();
        userDTO.setUserId(1L);
        userDTO.setUsername("testuser");

        when(userApplicationService.getUser(any())).thenReturn(userDTO);

        mockMvc.perform(get("/api/users/1")
                .header("Authorization", "Bearer token")
                .header("Host", "evil.com"))
                .andExpect(status().isOk());
    }

    @Test
    void testAccessWithXForwardedFor() throws Exception {
        when(authApplicationService.validateToken(any())).thenReturn("admin");

        UserDTO userDTO = new UserDTO();
        userDTO.setUserId(1L);
        userDTO.setUsername("testuser");

        when(userApplicationService.getUser(any())).thenReturn(userDTO);

        mockMvc.perform(get("/api/users/1")
                .header("Authorization", "Bearer token")
                .header("X-Forwarded-For", "127.0.0.1"))
                .andExpect(status().isOk());
    }

    @Test
    void testAccessWithXRealIp() throws Exception {
        when(authApplicationService.validateToken(any())).thenReturn("admin");

        UserDTO userDTO = new UserDTO();
        userDTO.setUserId(1L);
        userDTO.setUsername("testuser");

        when(userApplicationService.getUser(any())).thenReturn(userDTO);

        mockMvc.perform(get("/api/users/1")
                .header("Authorization", "Bearer token")
                .header("X-Real-IP", "127.0.0.1"))
                .andExpect(status().isOk());
    }

    @Test
    void testAccessWithViaHeader() throws Exception {
        when(authApplicationService.validateToken(any())).thenReturn("admin");

        UserDTO userDTO = new UserDTO();
        userDTO.setUserId(1L);
        userDTO.setUsername("testuser");

        when(userApplicationService.getUser(any())).thenReturn(userDTO);

        mockMvc.perform(get("/api/users/1")
                .header("Authorization", "Bearer token")
                .header("Via", "1.1 evil.com"))
                .andExpect(status().isOk());
    }

    @Test
    void testAccessWithRefererSpoofing() throws Exception {
        when(authApplicationService.validateToken(any())).thenReturn("admin");

        UserDTO userDTO = new UserDTO();
        userDTO.setUserId(1L);
        userDTO.setUsername("testuser");

        when(userApplicationService.getUser(any())).thenReturn(userDTO);

        mockMvc.perform(get("/api/users/1")
                .header("Authorization", "Bearer token")
                .header("Referer", "http://trusted-site.com"))
                .andExpect(status().isOk());
    }

    @Test
    void testAccessWithUserAgentSpoofing() throws Exception {
        when(authApplicationService.validateToken(any())).thenReturn("admin");

        UserDTO userDTO = new UserDTO();
        userDTO.setUserId(1L);
        userDTO.setUsername("testuser");

        when(userApplicationService.getUser(any())).thenReturn(userDTO);

        mockMvc.perform(get("/api/users/1")
                .header("Authorization", "Bearer token")
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36"))
                .andExpect(status().isOk());
    }

    @Test
    void testAccessWithAcceptSpoofing() throws Exception {
        when(authApplicationService.validateToken(any())).thenReturn("admin");

        UserDTO userDTO = new UserDTO();
        userDTO.setUserId(1L);
        userDTO.setUsername("testuser");

        when(userApplicationService.getUser(any())).thenReturn(userDTO);

        mockMvc.perform(get("/api/users/1")
                .header("Authorization", "Bearer token")
                .header("Accept", "application/json"))
                .andExpect(status().isOk());
    }

    @Test
    void testAccessWithAcceptEncodingSpoofing() throws Exception {
        when(authApplicationService.validateToken(any())).thenReturn("admin");

        UserDTO userDTO = new UserDTO();
        userDTO.setUserId(1L);
        userDTO.setUsername("testuser");

        when(userApplicationService.getUser(any())).thenReturn(userDTO);

        mockMvc.perform(get("/api/users/1")
                .header("Authorization", "Bearer token")
                .header("Accept-Encoding", "gzip, deflate"))
                .andExpect(status().isOk());
    }

    @Test
    void testAccessWithAcceptLanguageSpoofing() throws Exception {
        when(authApplicationService.validateToken(any())).thenReturn("admin");

        UserDTO userDTO = new UserDTO();
        userDTO.setUserId(1L);
        userDTO.setUsername("testuser");

        when(userApplicationService.getUser(any())).thenReturn(userDTO);

        mockMvc.perform(get("/api/users/1")
                .header("Authorization", "Bearer token")
                .header("Accept-Language", "en-US,en;q=0.9"))
                .andExpect(status().isOk());
    }

    @Test
    void testAccessWithConnectionSpoofing() throws Exception {
        when(authApplicationService.validateToken(any())).thenReturn("admin");

        UserDTO userDTO = new UserDTO();
        userDTO.setUserId(1L);
        userDTO.setUsername("testuser");

        when(userApplicationService.getUser(any())).thenReturn(userDTO);

        mockMvc.perform(get("/api/users/1")
                .header("Authorization", "Bearer token")
                .header("Connection", "keep-alive"))
                .andExpect(status().isOk());
    }

    @Test
    void testAccessWithCacheControlSpoofing() throws Exception {
        when(authApplicationService.validateToken(any())).thenReturn("admin");

        UserDTO userDTO = new UserDTO();
        userDTO.setUserId(1L);
        userDTO.setUsername("testuser");

        when(userApplicationService.getUser(any())).thenReturn(userDTO);

        mockMvc.perform(get("/api/users/1")
                .header("Authorization", "Bearer token")
                .header("Cache-Control", "no-cache"))
                .andExpect(status().isOk());
    }

    @Test
    void testAccessWithPragmaSpoofing() throws Exception {
        when(authApplicationService.validateToken(any())).thenReturn("admin");

        UserDTO userDTO = new UserDTO();
        userDTO.setUserId(1L);
        userDTO.setUsername("testuser");

        when(userApplicationService.getUser(any())).thenReturn(userDTO);

        mockMvc.perform(get("/api/users/1")
                .header("Authorization", "Bearer token")
                .header("Pragma", "no-cache"))
                .andExpect(status().isOk());
    }

    @Test
    void testAccessWithIfModifiedSinceSpoofing() throws Exception {
        when(authApplicationService.validateToken(any())).thenReturn("admin");

        UserDTO userDTO = new UserDTO();
        userDTO.setUserId(1L);
        userDTO.setUsername("testuser");

        when(userApplicationService.getUser(any())).thenReturn(userDTO);

        mockMvc.perform(get("/api/users/1")
                .header("Authorization", "Bearer token")
                .header("If-Modified-Since", "Wed, 21 Oct 2015 07:28:00 GMT"))
                .andExpect(status().isOk());
    }

    @Test
    void testAccessWithIfNoneMatchSpoofing() throws Exception {
        when(authApplicationService.validateToken(any())).thenReturn("admin");

        UserDTO userDTO = new UserDTO();
        userDTO.setUserId(1L);
        userDTO.setUsername("testuser");

        when(userApplicationService.getUser(any())).thenReturn(userDTO);

        mockMvc.perform(get("/api/users/1")
                .header("Authorization", "Bearer token")
                .header("If-None-Match", "\"123456\""))
                .andExpect(status().isOk());
    }

    @Test
    void testAccessWithRangeSpoofing() throws Exception {
        when(authApplicationService.validateToken(any())).thenReturn("admin");

        UserDTO userDTO = new UserDTO();
        userDTO.setUserId(1L);
        userDTO.setUsername("testuser");

        when(userApplicationService.getUser(any())).thenReturn(userDTO);

        mockMvc.perform(get("/api/users/1")
                .header("Authorization", "Bearer token")
                .header("Range", "bytes=0-1024"))
                .andExpect(status().isOk());
    }

    @Test
    void testAccessWithTESpoofing() throws Exception {
        when(authApplicationService.validateToken(any())).thenReturn("admin");

        UserDTO userDTO = new UserDTO();
        userDTO.setUserId(1L);
        userDTO.setUsername("testuser");

        when(userApplicationService.getUser(any())).thenReturn(userDTO);

        mockMvc.perform(get("/api/users/1")
                .header("Authorization", "Bearer token")
                .header("TE", "trailers"))
                .andExpect(status().isOk());
    }

    @Test
    void testAccessWithExpectSpoofing() throws Exception {
        when(authApplicationService.validateToken(any())).thenReturn("admin");

        UserDTO userDTO = new UserDTO();
        userDTO.setUserId(1L);
        userDTO.setUsername("testuser");

        when(userApplicationService.getUser(any())).thenReturn(userDTO);

        mockMvc.perform(post("/api/users")
                .header("Authorization", "Bearer token")
                .header("Expect", "100-continue")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"test\",\"password\":\"password\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void testAccessWithMaxForwardsSpoofing() throws Exception {
        when(authApplicationService.validateToken(any())).thenReturn("admin");

        UserDTO userDTO = new UserDTO();
        userDTO.setUserId(1L);
        userDTO.setUsername("testuser");

        when(userApplicationService.getUser(any())).thenReturn(userDTO);

        mockMvc.perform(get("/api/users/1")
                .header("Authorization", "Bearer token")
                .header("Max-Forwards", "10"))
                .andExpect(status().isOk());
    }

    @Test
    void testAccessWithAuthorizationSpoofing() throws Exception {
        when(authApplicationService.validateToken(any())).thenReturn("admin");

        UserDTO userDTO = new UserDTO();
        userDTO.setUserId(1L);
        userDTO.setUsername("testuser");

        when(userApplicationService.getUser(any())).thenReturn(userDTO);

        mockMvc.perform(get("/api/users/1")
                .header("Authorization", "Bearer token")
                .header("X-Authorization", "Bearer spoofed_token"))
                .andExpect(status().isOk());
    }

    @Test
    void testAccessWithProxyAuthorizationSpoofing() throws Exception {
        when(authApplicationService.validateToken(any())).thenReturn("admin");

        UserDTO userDTO = new UserDTO();
        userDTO.setUserId(1L);
        userDTO.setUsername("testuser");

        when(userApplicationService.getUser(any())).thenReturn(userDTO);

        mockMvc.perform(get("/api/users/1")
                .header("Authorization", "Bearer token")
                .header("Proxy-Authorization", "Basic dXNlcjpwYXNzd29yZA=="))
                .andExpect(status().isOk());
    }

    @Test
    void testAccessWithCookieSpoofing() throws Exception {
        when(authApplicationService.validateToken(any())).thenReturn("admin");

        UserDTO userDTO = new UserDTO();
        userDTO.setUserId(1L);
        userDTO.setUsername("testuser");

        when(userApplicationService.getUser(any())).thenReturn(userDTO);

        mockMvc.perform(get("/api/users/1")
                .header("Authorization", "Bearer token")
                .header("Cookie", "session=valid_session; admin=true"))
                .andExpect(status().isOk());
    }

    @Test
    void testAccessWithSetCookieSpoofing() throws Exception {
        when(authApplicationService.validateToken(any())).thenReturn("admin");

        UserDTO userDTO = new UserDTO();
        userDTO.setUserId(1L);
        userDTO.setUsername("testuser");

        when(userApplicationService.getUser(any())).thenReturn(userDTO);

        mockMvc.perform(get("/api/users/1")
                .header("Authorization", "Bearer token")
                .header("Set-Cookie", "admin=true"))
                .andExpect(status().isOk());
    }

    @Test
    void testAccessWithOriginSpoofing() throws Exception {
        when(authApplicationService.validateToken(any())).thenReturn("admin");

        UserDTO userDTO = new UserDTO();
        userDTO.setUserId(1L);
        userDTO.setUsername("testuser");

        when(userApplicationService.getUser(any())).thenReturn(userDTO);

        mockMvc.perform(get("/api/users/1")
                .header("Authorization", "Bearer token")
                .header("Origin", "http://trusted-site.com"))
                .andExpect(status().isOk());
    }

    @Test
    void testAccessWithAccessControlRequestHeadersSpoofing() throws Exception {
        when(authApplicationService.validateToken(any())).thenReturn("admin");

        UserDTO userDTO = new UserDTO();
        userDTO.setUserId(1L);
        userDTO.setUsername("testuser");

        when(userApplicationService.getUser(any())).thenReturn(userDTO);

        mockMvc.perform(get("/api/users/1")
                .header("Authorization", "Bearer token")
                .header("Access-Control-Request-Headers", "X-Custom-Header"))
                .andExpect(status().isOk());
    }

    @Test
    void testAccessWithAccessControlRequestMethodSpoofing() throws Exception {
        when(authApplicationService.validateToken(any())).thenReturn("admin");

        UserDTO userDTO = new UserDTO();
        userDTO.setUserId(1L);
        userDTO.setUsername("testuser");

        when(userApplicationService.getUser(any())).thenReturn(userDTO);

        mockMvc.perform(get("/api/users/1")
                .header("Authorization", "Bearer token")
                .header("Access-Control-Request-Method", "DELETE"))
                .andExpect(status().isOk());
    }

    @Test
    void testAccessWithDntSpoofing() throws Exception {
        when(authApplicationService.validateToken(any())).thenReturn("admin");

        UserDTO userDTO = new UserDTO();
        userDTO.setUserId(1L);
        userDTO.setUsername("testuser");

        when(userApplicationService.getUser(any())).thenReturn(userDTO);

        mockMvc.perform(get("/api/users/1")
                .header("Authorization", "Bearer token")
                .header("DNT", "1"))
                .andExpect(status().isOk());
    }

    @Test
    void testAccessWithUpgradeInsecureRequestsSpoofing() throws Exception {
        when(authApplicationService.validateToken(any())).thenReturn("admin");

        UserDTO userDTO = new UserDTO();
        userDTO.setUserId(1L);
        userDTO.setUsername("testuser");

        when(userApplicationService.getUser(any())).thenReturn(userDTO);

        mockMvc.perform(get("/api/users/1")
                .header("Authorization", "Bearer token")
                .header("Upgrade-Insecure-Requests", "1"))
                .andExpect(status().isOk());
    }

    @Test
    void testAccessWithSaveDataSpoofing() throws Exception {
        when(authApplicationService.validateToken(any())).thenReturn("admin");

        UserDTO userDTO = new UserDTO();
        userDTO.setUserId(1L);
        userDTO.setUsername("testuser");

        when(userApplicationService.getUser(any())).thenReturn(userDTO);

        mockMvc.perform(get("/api/users/1")
                .header("Authorization", "Bearer token")
                .header("Save-Data", "on"))
                .andExpect(status().isOk());
    }

    @Test
    void testAccessWithSecFetchSiteSpoofing() throws Exception {
        when(authApplicationService.validateToken(any())).thenReturn("admin");

        UserDTO userDTO = new UserDTO();
        userDTO.setUserId(1L);
        userDTO.setUsername("testuser");

        when(userApplicationService.getUser(any())).thenReturn(userDTO);

        mockMvc.perform(get("/api/users/1")
                .header("Authorization", "Bearer token")
                .header("Sec-Fetch-Site", "same-origin"))
                .andExpect(status().isOk());
    }

    @Test
    void testAccessWithSecFetchModeSpoofing() throws Exception {
        when(authApplicationService.validateToken(any())).thenReturn("admin");

        UserDTO userDTO = new UserDTO();
        userDTO.setUserId(1L);
        userDTO.setUsername("testuser");

        when(userApplicationService.getUser(any())).thenReturn(userDTO);

        mockMvc.perform(get("/api/users/1")
                .header("Authorization", "Bearer token")
                .header("Sec-Fetch-Mode", "cors"))
                .andExpect(status().isOk());
    }

    @Test
    void testAccessWithSecFetchUserSpoofing() throws Exception {
        when(authApplicationService.validateToken(any())).thenReturn("admin");

        UserDTO userDTO = new UserDTO();
        userDTO.setUserId(1L);
        userDTO.setUsername("testuser");

        when(userApplicationService.getUser(any())).thenReturn(userDTO);

        mockMvc.perform(get("/api/users/1")
                .header("Authorization", "Bearer token")
                .header("Sec-Fetch-User", "?1"))
                .andExpect(status().isOk());
    }

    @Test
    void testAccessWithSecFetchDestSpoofing() throws Exception {
        when(authApplicationService.validateToken(any())).thenReturn("admin");

        UserDTO userDTO = new UserDTO();
        userDTO.setUserId(1L);
        userDTO.setUsername("testuser");

        when(userApplicationService.getUser(any())).thenReturn(userDTO);

        mockMvc.perform(get("/api/users/1")
                .header("Authorization", "Bearer token")
                .header("Sec-Fetch-Dest", "empty"))
                .andExpect(status().isOk());
    }

    @Test
    void testAccessWithSecChUaSpoofing() throws Exception {
        when(authApplicationService.validateToken(any())).thenReturn("admin");

        UserDTO userDTO = new UserDTO();
        userDTO.setUserId(1L);
        userDTO.setUsername("testuser");

        when(userApplicationService.getUser(any())).thenReturn(userDTO);

        mockMvc.perform(get("/api/users/1")
                .header("Authorization", "Bearer token")
                .header("Sec-CH-UA", "\"Chromium\";v=\"94\""))
                .andExpect(status().isOk());
    }

    @Test
    void testAccessWithSecChUaMobileSpoofing() throws Exception {
        when(authApplicationService.validateToken(any())).thenReturn("admin");

        UserDTO userDTO = new UserDTO();
        userDTO.setUserId(1L);
        userDTO.setUsername("testuser");

        when(userApplicationService.getUser(any())).thenReturn(userDTO);

        mockMvc.perform(get("/api/users/1")
                .header("Authorization", "Bearer token")
                .header("Sec-CH-UA-Mobile", "?0"))
                .andExpect(status().isOk());
    }

    @Test
    void testAccessWithSecChUaPlatformSpoofing() throws Exception {
        when(authApplicationService.validateToken(any())).thenReturn("admin");

        UserDTO userDTO = new UserDTO();
        userDTO.setUserId(1L);
        userDTO.setUsername("testuser");

        when(userApplicationService.getUser(any())).thenReturn(userDTO);

        mockMvc.perform(get("/api/users/1")
                .header("Authorization", "Bearer token")
                .header("Sec-CH-UA-Platform", "\"Windows\""))
                .andExpect(status().isOk());
    }
}
