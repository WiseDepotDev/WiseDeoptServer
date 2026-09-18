package com.huicang.wise.api.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.huicang.wise.api.support.AbstractWebMvcSliceTest;
import com.huicang.wise.application.auth.AuthApplicationService;
import com.huicang.wise.application.user.UserApplicationService;
import com.huicang.wise.application.user.UserCreateRequest;
import com.huicang.wise.application.user.UserDTO;
import com.huicang.wise.application.user.UserPageDTO;
import com.huicang.wise.application.user.UserUpdateRequest;
import java.time.LocalDateTime;
import java.util.Collections;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@AutoConfigureMockMvc(addFilters = false)
@WebMvcTest(
        controllers = UserController.class,
        excludeFilters =
                @ComponentScan.Filter(
                        type = FilterType.REGEX,
                        pattern =
                                "com\\.huicang\\.wise\\.(application|infrastructure|domain)\\..*"))
public class UserControllerTest extends AbstractWebMvcSliceTest {

    @Autowired private MockMvc mockMvc;

    @MockBean private UserApplicationService userApplicationService;

    @MockBean private AuthApplicationService authApplicationService;

    @Autowired private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        when(authApplicationService.validateToken(any())).thenReturn("admin");
    }

    @Test
    void testCreateUser() throws Exception {
        UserCreateRequest request = new UserCreateRequest();
        request.setUsername("testuser");
        // nickname 为 @NotBlank 必填字段：@Valid 启用后缺省会返回 400（P2-08）
        request.setNickname("测试昵称");
        request.setPassword("password");
        request.setEmail("test@example.com");

        UserDTO userDTO = new UserDTO();
        userDTO.setUserId(1L);
        userDTO.setUsername("testuser");
        userDTO.setEmail("test@example.com");
        userDTO.setCreatedAt(LocalDateTime.now());

        when(userApplicationService.createUser(any(UserCreateRequest.class))).thenReturn(userDTO);

        mockMvc.perform(
                        post("/api/users")
                                .header("Authorization", "Bearer token")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.payload.code").value("RES-0000"))
                .andExpect(jsonPath("$.payload.data.userId").value(1))
                .andExpect(jsonPath("$.payload.data.username").value("testuser"));
    }

    @Test
    void testUpdateUser() throws Exception {
        Long userId = 1L;
        UserUpdateRequest request = new UserUpdateRequest();
        request.setEmail("updated@example.com");

        UserDTO userDTO = new UserDTO();
        userDTO.setUserId(userId);
        userDTO.setUsername("testuser");
        userDTO.setEmail("updated@example.com");

        when(userApplicationService.updateUser(any(UserUpdateRequest.class))).thenReturn(userDTO);

        mockMvc.perform(
                        put("/api/users/{userId}", userId)
                                .header("Authorization", "Bearer token")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.payload.code").value("RES-0000"))
                .andExpect(jsonPath("$.payload.data.email").value("updated@example.com"));
    }

    @Test
    void testGetUser() throws Exception {
        Long userId = 1L;
        UserDTO userDTO = new UserDTO();
        userDTO.setUserId(userId);
        userDTO.setUsername("testuser");

        when(userApplicationService.getUser(userId)).thenReturn(userDTO);

        mockMvc.perform(get("/api/users/{userId}", userId).header("Authorization", "Bearer token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.payload.code").value("RES-0000"))
                .andExpect(jsonPath("$.payload.data.userId").value(1));
    }

    @Test
    void testListUsers() throws Exception {
        UserPageDTO pageDTO = new UserPageDTO();
        UserDTO userDTO = new UserDTO();
        userDTO.setUserId(1L);
        userDTO.setUsername("testuser");
        pageDTO.setItems(Collections.singletonList(userDTO));
        pageDTO.setTotal(1L);

        when(userApplicationService.listUsers(any(), any())).thenReturn(pageDTO);

        mockMvc.perform(get("/api/users").header("Authorization", "Bearer token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.payload.code").value("RES-0000"))
                .andExpect(jsonPath("$.payload.data.items[0].userId").value(1));
    }

    @Test
    void testDeleteUser() throws Exception {
        Long userId = 1L;
        doNothing().when(userApplicationService).deleteUser(userId);

        mockMvc.perform(
                        delete("/api/users/{userId}", userId)
                                .header("Authorization", "Bearer token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.payload.code").value("RES-0000"));
    }

    /**
     * 参数校验必须生效：username 为 @NotBlank，空值应被拒绝（P2-08）。
     *
     * <p>此前控制器未标注 @Valid，DTO 上的 @NotBlank/@Size 等注解形同虚设； 本用例锁定「校验真的在入口执行」这一行为，防止回退。
     *
     * @throws Exception 请求执行异常
     */
    @Test
    void testCreateUserShouldRejectBlankUsername() throws Exception {
        UserCreateRequest request = new UserCreateRequest();
        request.setUsername("");
        request.setNickname("昵称");
        request.setPassword("password");
        request.setEmail("test@example.com");

        mockMvc.perform(
                        post("/api/users")
                                .header("Authorization", "Bearer token")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().is4xxClientError());
    }
}
