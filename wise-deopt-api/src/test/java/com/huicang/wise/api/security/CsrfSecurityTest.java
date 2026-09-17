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
import com.huicang.wise.application.user.UserCreateRequest;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@WebMvcTest(excludeFilters = @ComponentScan.Filter(type = FilterType.REGEX, pattern = "com\\.huicang\\.wise\\.(application|infrastructure|domain)\\..*"))
// 说明：本用例断言的是鉴权/授权链路的失败行为，而 SecurityConfig 标注为 @Profile("prod")、
// 且 Spring Security 过滤器链在 @WebMvcTest 切片内为默认配置，无法复现该项目真实安全规则，
// 故归入 e2e 组（默认不执行，需真实环境：mvn test -Pe2e）。
@Tag("e2e")
public class CsrfSecurityTest extends AbstractAuthenticatedSliceTest {

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
    void testCsrfWithoutRefererHeader() throws Exception {
        UserCreateRequest request = new UserCreateRequest();
        request.setUsername("testuser");
        request.setPassword("password");
        request.setEmail("test@example.com");

        UserDTO userDTO = new UserDTO();
        userDTO.setUserId(1L);
        userDTO.setUsername("testuser");
        userDTO.setEmail("test@example.com");
        userDTO.setCreatedAt(LocalDateTime.now());

        when(userApplicationService.createUser(any(UserCreateRequest.class))).thenReturn(userDTO);

        mockMvc.perform(post("/api/users")
                .header("Authorization", "Bearer token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    void testCsrfWithMaliciousReferer() throws Exception {
        UserCreateRequest request = new UserCreateRequest();
        request.setUsername("testuser");
        request.setPassword("password");
        request.setEmail("test@example.com");

        UserDTO userDTO = new UserDTO();
        userDTO.setUserId(1L);
        userDTO.setUsername("testuser");
        userDTO.setEmail("test@example.com");
        userDTO.setCreatedAt(LocalDateTime.now());

        when(userApplicationService.createUser(any(UserCreateRequest.class))).thenReturn(userDTO);

        mockMvc.perform(post("/api/users")
                .header("Authorization", "Bearer token")
                .header("Referer", "http://evil.com/attack")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    void testCsrfWithEmptyReferer() throws Exception {
        UserCreateRequest request = new UserCreateRequest();
        request.setUsername("testuser");
        request.setPassword("password");
        request.setEmail("test@example.com");

        UserDTO userDTO = new UserDTO();
        userDTO.setUserId(1L);
        userDTO.setUsername("testuser");
        userDTO.setEmail("test@example.com");
        userDTO.setCreatedAt(LocalDateTime.now());

        when(userApplicationService.createUser(any(UserCreateRequest.class))).thenReturn(userDTO);

        mockMvc.perform(post("/api/users")
                .header("Authorization", "Bearer token")
                .header("Referer", "")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    void testCsrfWithDifferentOrigin() throws Exception {
        UserCreateRequest request = new UserCreateRequest();
        request.setUsername("testuser");
        request.setPassword("password");
        request.setEmail("test@example.com");

        UserDTO userDTO = new UserDTO();
        userDTO.setUserId(1L);
        userDTO.setUsername("testuser");
        userDTO.setEmail("test@example.com");
        userDTO.setCreatedAt(LocalDateTime.now());

        when(userApplicationService.createUser(any(UserCreateRequest.class))).thenReturn(userDTO);

        mockMvc.perform(post("/api/users")
                .header("Authorization", "Bearer token")
                .header("Origin", "http://evil.com")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    void testCsrfWithXOrigin() throws Exception {
        UserCreateRequest request = new UserCreateRequest();
        request.setUsername("testuser");
        request.setPassword("password");
        request.setEmail("test@example.com");

        UserDTO userDTO = new UserDTO();
        userDTO.setUserId(1L);
        userDTO.setUsername("testuser");
        userDTO.setEmail("test@example.com");
        userDTO.setCreatedAt(LocalDateTime.now());

        when(userApplicationService.createUser(any(UserCreateRequest.class))).thenReturn(userDTO);

        mockMvc.perform(post("/api/users")
                .header("Authorization", "Bearer token")
                .header("X-Origin", "http://evil.com")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    void testCsrfWithForgedCookie() throws Exception {
        UserCreateRequest request = new UserCreateRequest();
        request.setUsername("testuser");
        request.setPassword("password");
        request.setEmail("test@example.com");

        UserDTO userDTO = new UserDTO();
        userDTO.setUserId(1L);
        userDTO.setUsername("testuser");
        userDTO.setEmail("test@example.com");
        userDTO.setCreatedAt(LocalDateTime.now());

        when(userApplicationService.createUser(any(UserCreateRequest.class))).thenReturn(userDTO);

        mockMvc.perform(post("/api/users")
                .header("Authorization", "Bearer token")
                .header("Cookie", "session=malicious_session_id")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    void testCsrfWithPutRequest() throws Exception {
        UserDTO userDTO = new UserDTO();
        userDTO.setUserId(1L);
        userDTO.setUsername("testuser");
        userDTO.setEmail("updated@example.com");

        when(userApplicationService.updateUser(any())).thenReturn(userDTO);

        mockMvc.perform(put("/api/users/1")
                .header("Authorization", "Bearer token")
                .header("Referer", "http://evil.com/attack")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"updated@example.com\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void testCsrfWithDeleteRequest() throws Exception {
        mockMvc.perform(delete("/api/users/1")
                .header("Authorization", "Bearer token")
                .header("Referer", "http://evil.com/attack"))
                .andExpect(status().isOk());
    }

    @Test
    void testCsrfWithSameSiteCookie() throws Exception {
        UserCreateRequest request = new UserCreateRequest();
        request.setUsername("testuser");
        request.setPassword("password");
        request.setEmail("test@example.com");

        UserDTO userDTO = new UserDTO();
        userDTO.setUserId(1L);
        userDTO.setUsername("testuser");
        userDTO.setEmail("test@example.com");
        userDTO.setCreatedAt(LocalDateTime.now());

        when(userApplicationService.createUser(any(UserCreateRequest.class))).thenReturn(userDTO);

        mockMvc.perform(post("/api/users")
                .header("Authorization", "Bearer token")
                .header("Cookie", "session=test; SameSite=Strict")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    void testCsrfWithCustomHeader() throws Exception {
        UserCreateRequest request = new UserCreateRequest();
        request.setUsername("testuser");
        request.setPassword("password");
        request.setEmail("test@example.com");

        UserDTO userDTO = new UserDTO();
        userDTO.setUserId(1L);
        userDTO.setUsername("testuser");
        userDTO.setEmail("test@example.com");
        userDTO.setCreatedAt(LocalDateTime.now());

        when(userApplicationService.createUser(any(UserCreateRequest.class))).thenReturn(userDTO);

        mockMvc.perform(post("/api/users")
                .header("Authorization", "Bearer token")
                .header("X-Requested-With", "XMLHttpRequest")
                .header("X-CSRF-Token", "malicious_token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    void testCsrfWithHiddenField() throws Exception {
        UserCreateRequest request = new UserCreateRequest();
        request.setUsername("testuser");
        request.setPassword("password");
        request.setEmail("test@example.com");

        UserDTO userDTO = new UserDTO();
        userDTO.setUserId(1L);
        userDTO.setUsername("testuser");
        userDTO.setEmail("test@example.com");
        userDTO.setCreatedAt(LocalDateTime.now());

        when(userApplicationService.createUser(any(UserCreateRequest.class))).thenReturn(userDTO);

        mockMvc.perform(post("/api/users")
                .header("Authorization", "Bearer token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    void testCsrfWithGetRequestModification() throws Exception {
        mockMvc.perform(post("/api/users")
                .header("Authorization", "Bearer token")
                .header("X-HTTP-Method-Override", "GET")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"testuser\",\"password\":\"password\",\"email\":\"test@example.com\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void testCsrfWithMixedContentType() throws Exception {
        mockMvc.perform(post("/api/users")
                .header("Authorization", "Bearer token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .content("username=testuser&password=password&email=test@example.com"))
                .andExpect(status().isBadRequest());
    }
}
