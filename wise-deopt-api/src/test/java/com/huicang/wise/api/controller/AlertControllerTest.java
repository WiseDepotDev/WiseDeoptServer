package com.huicang.wise.api.controller;

import java.time.LocalDateTime;
import java.util.Collections;

import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
import com.huicang.wise.application.alert.AlertApplicationService;
import com.huicang.wise.application.alert.AlertCreateRequest;
import com.huicang.wise.application.alert.AlertDTO;
import com.huicang.wise.application.alert.AlertEventPageDTO;
import com.huicang.wise.application.alert.AlertEventSummaryDTO;
import com.huicang.wise.application.alert.UpdateAlertStatusRequest;
import com.huicang.wise.application.auth.AuthApplicationService;
import org.junit.jupiter.api.BeforeEach;

@WebMvcTest(controllers = AlertController.class, excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JpaConfiguration.class))
public class AlertControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AlertApplicationService alertApplicationService;

    @MockBean
    private AuthApplicationService authApplicationService;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        when(authApplicationService.validateToken(any())).thenReturn("admin");
    }

    @Test
    void testCreateAlert() throws Exception {
        AlertCreateRequest request = new AlertCreateRequest();
        request.setDescription("Test Alert");
        request.setAlertType("VIOLATION");
        request.setAlertLevel("HIGH");

        AlertDTO dto = new AlertDTO();
        dto.setEventId(1L);
        dto.setDescription("Test Alert");
        dto.setAlertType("VIOLATION");
        dto.setAlertLevel("HIGH");
        dto.setAlertTime(LocalDateTime.now());

        when(alertApplicationService.createAlert(any(AlertCreateRequest.class))).thenReturn(dto);

        mockMvc.perform(post("/api/alerts")
                .header("Authorization", "Bearer token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.body.payload.code").value("RES-0000"))
                .andExpect(jsonPath("$.body.payload.data.eventId").value(1))
                .andExpect(jsonPath("$.body.payload.data.description").value("Test Alert"));
    }

    @Test
    void testUpdateAlert() throws Exception {
        Long eventId = 1L;
        UpdateAlertStatusRequest request = new UpdateAlertStatusRequest();
        request.setStatus(1);

        doNothing().when(alertApplicationService).updateAlertStatus(eq(eventId), any(UpdateAlertStatusRequest.class));

        mockMvc.perform(put("/api/alerts/{eventId}/status", eventId)
                .header("Authorization", "Bearer token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.body.payload.code").value("RES-0000"));
    }

    @Test
    void testListAlerts() throws Exception {
        AlertEventPageDTO pageDTO = new AlertEventPageDTO();
        AlertEventSummaryDTO summaryDTO = new AlertEventSummaryDTO();
        summaryDTO.setEventId(1L);
        summaryDTO.setMessage("Test Alert");
        pageDTO.setRows(Collections.singletonList(summaryDTO));
        pageDTO.setTotal(1L);

        when(alertApplicationService.listAlertEvents(any(), any(), any(), any(), any(), any())).thenReturn(pageDTO);

        mockMvc.perform(get("/api/alerts")
                .header("Authorization", "Bearer token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.body.payload.code").value("RES-0000"))
                .andExpect(jsonPath("$.body.payload.data.rows[0].eventId").value(1))
                .andExpect(jsonPath("$.body.payload.data.rows[0].message").value("Test Alert"));
    }

    @Test
    void testGetAlert() throws Exception {
        Long eventId = 1L;
        AlertDTO dto = new AlertDTO();
        dto.setEventId(eventId);
        dto.setDescription("Test Alert");

        when(alertApplicationService.getAlert(eventId)).thenReturn(dto);

        mockMvc.perform(get("/api/alerts/{eventId}", eventId)
                .header("Authorization", "Bearer token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.body.payload.code").value("RES-0000"))
                .andExpect(jsonPath("$.body.payload.data.eventId").value(1));
    }
}
