package com.huicang.wise.api.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.huicang.wise.application.auth.AuthApplicationService;
import com.huicang.wise.application.user.UserApplicationService;
import com.huicang.wise.application.user.UserCreateRequest;
import com.huicang.wise.application.user.UserDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest
public class SqlInjectionSecurityTest {

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
    void testSqlInjectionInUsername() throws Exception {
        UserCreateRequest request = new UserCreateRequest();
        request.setUsername("admin' OR '1'='1");
        request.setPassword("password");
        request.setEmail("test@example.com");

        UserDTO userDTO = new UserDTO();
        userDTO.setUserId(1L);
        userDTO.setUsername("admin' OR '1'='1");
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
    void testSqlInjectionInEmail() throws Exception {
        UserCreateRequest request = new UserCreateRequest();
        request.setUsername("testuser");
        request.setPassword("password");
        request.setEmail("test@example.com' OR '1'='1");

        UserDTO userDTO = new UserDTO();
        userDTO.setUserId(1L);
        userDTO.setUsername("testuser");
        userDTO.setEmail("test@example.com' OR '1'='1");
        userDTO.setCreatedAt(LocalDateTime.now());

        when(userApplicationService.createUser(any(UserCreateRequest.class))).thenReturn(userDTO);

        mockMvc.perform(post("/api/users")
                .header("Authorization", "Bearer token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    void testSqlInjectionInQueryParameter() throws Exception {
        UserDTO userDTO = new UserDTO();
        userDTO.setUserId(1L);
        userDTO.setUsername("testuser");

        when(userApplicationService.getUser(any())).thenReturn(userDTO);

        mockMvc.perform(get("/api/users/1' OR '1'='1")
                .header("Authorization", "Bearer token"))
                .andExpect(status().isOk());
    }

    @Test
    void testSqlInjectionWithUnion() throws Exception {
        UserDTO userDTO = new UserDTO();
        userDTO.setUserId(1L);
        userDTO.setUsername("testuser");

        when(userApplicationService.getUser(any())).thenReturn(userDTO);

        mockMvc.perform(get("/api/users/1' UNION SELECT * FROM users--")
                .header("Authorization", "Bearer token"))
                .andExpect(status().isOk());
    }

    @Test
    void testSqlInjectionWithComment() throws Exception {
        UserCreateRequest request = new UserCreateRequest();
        request.setUsername("admin'--");
        request.setPassword("password");
        request.setEmail("test@example.com");

        UserDTO userDTO = new UserDTO();
        userDTO.setUserId(1L);
        userDTO.setUsername("admin'--");
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
    void testSqlInjectionWithSemicolon() throws Exception {
        UserCreateRequest request = new UserCreateRequest();
        request.setUsername("admin'; DROP TABLE users;--");
        request.setPassword("password");
        request.setEmail("test@example.com");

        UserDTO userDTO = new UserDTO();
        userDTO.setUserId(1L);
        userDTO.setUsername("admin'; DROP TABLE users;--");
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
    void testSqlInjectionWithTimeBasedBlind() throws Exception {
        UserDTO userDTO = new UserDTO();
        userDTO.setUserId(1L);
        userDTO.setUsername("testuser");

        when(userApplicationService.getUser(any())).thenReturn(userDTO);

        mockMvc.perform(get("/api/users/1' AND SLEEP(5)--")
                .header("Authorization", "Bearer token"))
                .andExpect(status().isOk());
    }

    @Test
    void testSqlInjectionWithBooleanBasedBlind() throws Exception {
        UserDTO userDTO = new UserDTO();
        userDTO.setUserId(1L);
        userDTO.setUsername("testuser");

        when(userApplicationService.getUser(any())).thenReturn(userDTO);

        mockMvc.perform(get("/api/users/1' AND 1=1--")
                .header("Authorization", "Bearer token"))
                .andExpect(status().isOk());
    }

    @Test
    void testSqlInjectionWithStackedQueries() throws Exception {
        UserCreateRequest request = new UserCreateRequest();
        request.setUsername("admin'; SELECT * FROM information_schema.tables;--");
        request.setPassword("password");
        request.setEmail("test@example.com");

        UserDTO userDTO = new UserDTO();
        userDTO.setUserId(1L);
        userDTO.setUsername("admin'; SELECT * FROM information_schema.tables;--");
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
    void testSqlInjectionWithHexEncoding() throws Exception {
        UserDTO userDTO = new UserDTO();
        userDTO.setUserId(1L);
        userDTO.setUsername("testuser");

        when(userApplicationService.getUser(any())).thenReturn(userDTO);

        mockMvc.perform(get("/api/users/0x31")
                .header("Authorization", "Bearer token"))
                .andExpect(status().isOk());
    }
}
