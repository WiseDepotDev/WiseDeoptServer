package com.huicang.wise.api.controller;

import java.time.LocalDateTime;
import java.util.Collections;

import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.huicang.wise.api.config.JpaConfiguration;
import com.huicang.wise.application.user.UserApplicationService;
import com.huicang.wise.application.user.UserCreateRequest;
import com.huicang.wise.application.user.UserDTO;
import com.huicang.wise.application.user.UserPageDTO;
import com.huicang.wise.application.user.UserUpdateRequest;

import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;

@WebMvcTest(controllers = UserController.class)
public class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserApplicationService userApplicationService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void testCreateUser() throws Exception {
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
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.body.payload.code").value("RES-0000"))
                .andExpect(jsonPath("$.body.payload.data.userId").value(1))
                .andExpect(jsonPath("$.body.payload.data.username").value("testuser"));
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

        mockMvc.perform(put("/api/users/{userId}", userId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.body.payload.code").value("RES-0000"))
                .andExpect(jsonPath("$.body.payload.data.email").value("updated@example.com"));
    }

    @Test
    void testGetUser() throws Exception {
        Long userId = 1L;
        UserDTO userDTO = new UserDTO();
        userDTO.setUserId(userId);
        userDTO.setUsername("testuser");

        when(userApplicationService.getUser(userId)).thenReturn(userDTO);

        mockMvc.perform(get("/api/users/{userId}", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.body.payload.code").value("RES-0000"))
                .andExpect(jsonPath("$.body.payload.data.userId").value(1));
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

        mockMvc.perform(get("/api/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.body.payload.code").value("RES-0000"))
                .andExpect(jsonPath("$.body.payload.data.items[0].userId").value(1));
    }

    @Test
    void testDeleteUser() throws Exception {
        Long userId = 1L;
        doNothing().when(userApplicationService).deleteUser(userId);

        mockMvc.perform(delete("/api/users/{userId}", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.body.payload.code").value("RES-0000"));
    }
}
