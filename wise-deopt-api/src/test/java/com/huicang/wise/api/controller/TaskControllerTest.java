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
import com.huicang.wise.application.task.TaskApplicationService;
import com.huicang.wise.application.task.TaskCreateRequest;
import com.huicang.wise.application.task.TaskDTO;
import com.huicang.wise.application.task.TaskUpdateRequest;

import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;

@WebMvcTest(controllers = TaskController.class)
public class TaskControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TaskApplicationService taskApplicationService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void testCreateTask() throws Exception {
        TaskCreateRequest request = new TaskCreateRequest();
        request.setTaskName("Test Task");
        request.setTaskType(1);
        request.setCreatedBy("admin");

        TaskDTO taskDTO = new TaskDTO();
        taskDTO.setTaskId(1L);
        taskDTO.setTaskName("Test Task");
        taskDTO.setTaskType(1);
        taskDTO.setStatus(0);
        taskDTO.setCreatedAt(LocalDateTime.now());

        when(taskApplicationService.createTask(any(TaskCreateRequest.class))).thenReturn(taskDTO);

        mockMvc.perform(post("/api/tasks")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.body.payload.code").value("RES-0000"))
                .andExpect(jsonPath("$.body.payload.data.taskId").value(1))
                .andExpect(jsonPath("$.body.payload.data.taskName").value("Test Task"));
    }

    @Test
    void testUpdateTask() throws Exception {
        Long taskId = 1L;
        TaskUpdateRequest request = new TaskUpdateRequest();
        request.setTaskName("Updated Task");
        request.setStatus(1);

        TaskDTO taskDTO = new TaskDTO();
        taskDTO.setTaskId(taskId);
        taskDTO.setTaskName("Updated Task");
        taskDTO.setStatus(1);

        when(taskApplicationService.updateTask(any(TaskUpdateRequest.class))).thenReturn(taskDTO);

        mockMvc.perform(put("/api/tasks/{taskId}", taskId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.body.payload.code").value("RES-0000"))
                .andExpect(jsonPath("$.body.payload.data.taskName").value("Updated Task"));
    }

    @Test
    void testListTasks() throws Exception {
        TaskDTO taskDTO = new TaskDTO();
        taskDTO.setTaskId(1L);
        taskDTO.setTaskName("Test Task");

        when(taskApplicationService.listTasks()).thenReturn(Collections.singletonList(taskDTO));

        mockMvc.perform(get("/api/tasks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.body.payload.code").value("RES-0000"))
                .andExpect(jsonPath("$.body.payload.data[0].taskId").value(1));
    }

    @Test
    void testGetTask() throws Exception {
        Long taskId = 1L;
        TaskDTO taskDTO = new TaskDTO();
        taskDTO.setTaskId(taskId);
        taskDTO.setTaskName("Test Task");

        when(taskApplicationService.getTask(taskId)).thenReturn(taskDTO);

        mockMvc.perform(get("/api/tasks/{taskId}", taskId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.body.payload.code").value("RES-0000"))
                .andExpect(jsonPath("$.body.payload.data.taskId").value(1));
    }

    @Test
    void testDeleteTask() throws Exception {
        Long taskId = 1L;
        doNothing().when(taskApplicationService).deleteTask(taskId);

        mockMvc.perform(delete("/api/tasks/{taskId}", taskId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.body.payload.code").value("RES-0000"));
    }
}
