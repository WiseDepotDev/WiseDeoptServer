package com.huicang.wise.api.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.huicang.wise.api.support.AbstractWebMvcSliceTest;
import com.huicang.wise.application.alert.AlertCreateRequest;
import com.huicang.wise.application.alert.AlertDTO;
import com.huicang.wise.application.alert.AlertEventPageDTO;
import com.huicang.wise.application.alert.AlertEventSummaryDTO;
import com.huicang.wise.application.alert.UpdateAlertStatusRequest;
import com.huicang.wise.application.auth.AuthApplicationService;
import com.huicang.wise.infrastructure.config.JpaConfiguration;
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
        controllers = AlertController.class,
        excludeFilters = {
            @ComponentScan.Filter(
                    type = FilterType.ASSIGNABLE_TYPE,
                    classes = JpaConfiguration.class),
            @ComponentScan.Filter(
                    type = FilterType.REGEX,
                    pattern = "com\\.huicang\\.wise\\.(application|infrastructure|domain)\\..*")
        })
public class AlertControllerTest extends AbstractWebMvcSliceTest {

    @Autowired private MockMvc mockMvc;
    @MockBean private AuthApplicationService authApplicationService;

    @Autowired private ObjectMapper objectMapper;

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
        dto.setMessage("Test Alert");
        dto.setSourceModule("MANUAL");
        dto.setLevel(3);
        dto.setCreateTime(LocalDateTime.now());

        when(alertApplicationService.createAlert(any(AlertCreateRequest.class))).thenReturn(dto);

        mockMvc.perform(
                        post("/api/alerts")
                                .header("Authorization", "Bearer token")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.payload.code").value("RES-0000"))
                .andExpect(jsonPath("$.payload.data.eventId").value(1))
                .andExpect(jsonPath("$.payload.data.message").value("Test Alert"));
    }

    @Test
    void testUpdateAlert() throws Exception {
        Long eventId = 1L;
        UpdateAlertStatusRequest request = new UpdateAlertStatusRequest();
        request.setStatus(1);

        doNothing()
                .when(alertApplicationService)
                .updateAlertStatus(eq(eventId), any(UpdateAlertStatusRequest.class));

        mockMvc.perform(
                        put("/api/alerts/{eventId}/status", eventId)
                                .header("Authorization", "Bearer token")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.payload.code").value("RES-0000"));
    }

    @Test
    void testListAlerts() throws Exception {
        AlertEventPageDTO pageDTO = new AlertEventPageDTO();
        AlertEventSummaryDTO summaryDTO = new AlertEventSummaryDTO();
        summaryDTO.setEventId(1L);
        summaryDTO.setMessage("Test Alert");
        pageDTO.setRows(Collections.singletonList(summaryDTO));
        pageDTO.setTotal(1L);

        when(alertApplicationService.listAlertEvents(any(), any(), any(), any(), any(), any()))
                .thenReturn(pageDTO);

        mockMvc.perform(get("/api/alerts").header("Authorization", "Bearer token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.payload.code").value("RES-0000"))
                .andExpect(jsonPath("$.payload.data.rows[0].eventId").value(1))
                .andExpect(jsonPath("$.payload.data.rows[0].message").value("Test Alert"));
    }

    @Test
    void testGetAlert() throws Exception {
        Long eventId = 1L;
        AlertDTO dto = new AlertDTO();
        dto.setEventId(eventId);
        dto.setMessage("Test Alert");

        when(alertApplicationService.getAlert(eventId)).thenReturn(dto);

        mockMvc.perform(
                        get("/api/alerts/{eventId}", eventId)
                                .header("Authorization", "Bearer token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.payload.code").value("RES-0000"))
                .andExpect(jsonPath("$.payload.data.eventId").value(1));
    }
}
