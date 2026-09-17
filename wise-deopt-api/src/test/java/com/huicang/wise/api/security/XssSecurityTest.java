package com.huicang.wise.api.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.huicang.wise.api.support.AbstractWebMvcSliceTest;
import com.huicang.wise.application.auth.AuthApplicationService;
import com.huicang.wise.api.support.AbstractWebMvcSliceTest;
import com.huicang.wise.application.user.UserApplicationService;
import com.huicang.wise.api.support.AbstractWebMvcSliceTest;
import com.huicang.wise.application.user.UserCreateRequest;
import com.huicang.wise.api.support.AbstractWebMvcSliceTest;
import com.huicang.wise.application.user.UserDTO;
import org.junit.jupiter.api.BeforeEach;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@WebMvcTest(excludeFilters = @ComponentScan.Filter(type = FilterType.REGEX, pattern = "com\\.huicang\\.wise\\.(application|infrastructure|domain)\\..*"))
public class XssSecurityTest extends AbstractWebMvcSliceTest {

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
    void testXssWithScriptTag() throws Exception {
        UserCreateRequest request = new UserCreateRequest();
        request.setUsername("<script>alert('XSS')</script>");
        request.setPassword("password");
        request.setEmail("test@example.com");

        UserDTO userDTO = new UserDTO();
        userDTO.setUserId(1L);
        userDTO.setUsername("<script>alert('XSS')</script>");
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
    void testXssWithImgTag() throws Exception {
        UserCreateRequest request = new UserCreateRequest();
        request.setUsername("<img src=x onerror=alert('XSS')>");
        request.setPassword("password");
        request.setEmail("test@example.com");

        UserDTO userDTO = new UserDTO();
        userDTO.setUserId(1L);
        userDTO.setUsername("<img src=x onerror=alert('XSS')>");
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
    void testXssWithOnclickAttribute() throws Exception {
        UserCreateRequest request = new UserCreateRequest();
        request.setUsername("<div onclick=\"alert('XSS')\">Click me</div>");
        request.setPassword("password");
        request.setEmail("test@example.com");

        UserDTO userDTO = new UserDTO();
        userDTO.setUserId(1L);
        userDTO.setUsername("<div onclick=\"alert('XSS')\">Click me</div>");
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
    void testXssWithJavascriptProtocol() throws Exception {
        UserCreateRequest request = new UserCreateRequest();
        request.setUsername("<a href=\"javascript:alert('XSS')\">Click me</a>");
        request.setPassword("password");
        request.setEmail("test@example.com");

        UserDTO userDTO = new UserDTO();
        userDTO.setUserId(1L);
        userDTO.setUsername("<a href=\"javascript:alert('XSS')\">Click me</a>");
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
    void testXssWithSvgTag() throws Exception {
        UserCreateRequest request = new UserCreateRequest();
        request.setUsername("<svg onload=alert('XSS')>");
        request.setPassword("password");
        request.setEmail("test@example.com");

        UserDTO userDTO = new UserDTO();
        userDTO.setUserId(1L);
        userDTO.setUsername("<svg onload=alert('XSS')>");
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
    void testXssWithIframeTag() throws Exception {
        UserCreateRequest request = new UserCreateRequest();
        request.setUsername("<iframe src=\"javascript:alert('XSS')\"></iframe>");
        request.setPassword("password");
        request.setEmail("test@example.com");

        UserDTO userDTO = new UserDTO();
        userDTO.setUserId(1L);
        userDTO.setUsername("<iframe src=\"javascript:alert('XSS')\"></iframe>");
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
    void testXssWithBodyTag() throws Exception {
        UserCreateRequest request = new UserCreateRequest();
        request.setUsername("<body onload=alert('XSS')>");
        request.setPassword("password");
        request.setEmail("test@example.com");

        UserDTO userDTO = new UserDTO();
        userDTO.setUserId(1L);
        userDTO.setUsername("<body onload=alert('XSS')>");
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
    void testXssWithInputTag() throws Exception {
        UserCreateRequest request = new UserCreateRequest();
        request.setUsername("<input type=\"text\" value=\"\" onfocus=alert('XSS') autofocus>");
        request.setPassword("password");
        request.setEmail("test@example.com");

        UserDTO userDTO = new UserDTO();
        userDTO.setUserId(1L);
        userDTO.setUsername("<input type=\"text\" value=\"\" onfocus=alert('XSS') autofocus>");
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
    void testXssWithDetailsTag() throws Exception {
        UserCreateRequest request = new UserCreateRequest();
        request.setUsername("<details open ontoggle=alert('XSS')>");
        request.setPassword("password");
        request.setEmail("test@example.com");

        UserDTO userDTO = new UserDTO();
        userDTO.setUserId(1L);
        userDTO.setUsername("<details open ontoggle=alert('XSS')>");
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
    void testXssWithMarqueeTag() throws Exception {
        UserCreateRequest request = new UserCreateRequest();
        request.setUsername("<marquee onstart=alert('XSS')>");
        request.setPassword("password");
        request.setEmail("test@example.com");

        UserDTO userDTO = new UserDTO();
        userDTO.setUserId(1L);
        userDTO.setUsername("<marquee onstart=alert('XSS')>");
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
    void testXssWithEncodedScript() throws Exception {
        UserCreateRequest request = new UserCreateRequest();
        request.setUsername("&lt;script&gt;alert('XSS')&lt;/script&gt;");
        request.setPassword("password");
        request.setEmail("test@example.com");

        UserDTO userDTO = new UserDTO();
        userDTO.setUserId(1L);
        userDTO.setUsername("&lt;script&gt;alert('XSS')&lt;/script&gt;");
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
    void testXssWithUnicodeEncoding() throws Exception {
        UserCreateRequest request = new UserCreateRequest();
        request.setUsername("\\u003cscript\\u003ealert('XSS')\\u003c/script\\u003e");
        request.setPassword("password");
        request.setEmail("test@example.com");

        UserDTO userDTO = new UserDTO();
        userDTO.setUserId(1L);
        userDTO.setUsername("\\u003cscript\\u003ealert('XSS')\\u003c/script\\u003e");
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
    void testXssWithHexEncoding() throws Exception {
        UserCreateRequest request = new UserCreateRequest();
        request.setUsername("%3Cscript%3Ealert('XSS')%3C/script%3E");
        request.setPassword("password");
        request.setEmail("test@example.com");

        UserDTO userDTO = new UserDTO();
        userDTO.setUserId(1L);
        userDTO.setUsername("%3Cscript%3Ealert('XSS')%3C/script%3E");
        userDTO.setEmail("test@example.com");
        userDTO.setCreatedAt(LocalDateTime.now());

        when(userApplicationService.createUser(any(UserCreateRequest.class))).thenReturn(userDTO);

        mockMvc.perform(post("/api/users")
                .header("Authorization", "Bearer token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }
}
