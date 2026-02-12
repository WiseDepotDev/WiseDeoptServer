package com.huicang.wise.api.controller;

import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.when;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.huicang.wise.api.config.JpaConfiguration;
import com.huicang.wise.application.dashboard.DashboardApplicationService;
import com.huicang.wise.application.dashboard.DashboardSummaryDTO;
import java.util.Collections;
import com.huicang.wise.application.alert.AlertDTO;
import com.huicang.wise.application.task.TaskDTO;

import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import com.huicang.wise.application.auth.AuthApplicationService;
import org.junit.jupiter.api.BeforeEach;
import static org.mockito.ArgumentMatchers.any;

@WebMvcTest(controllers = DashboardController.class, excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JpaConfiguration.class))
public class DashboardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DashboardApplicationService dashboardApplicationService;

    @MockBean
    private AuthApplicationService authApplicationService;

    @BeforeEach
    void setUp() {
        when(authApplicationService.validateToken(any())).thenReturn("admin");
    }

    @Test
    void testGetSummary() throws Exception {
        DashboardSummaryDTO summary = new DashboardSummaryDTO();
        summary.setInventoryTotal(1000L);
        summary.setTodayAlertCount(5L);
        summary.setInspectionProgress(80);
        summary.setDeviceOnlineCount(10L);
        
        AlertDTO alert = new AlertDTO();
        alert.setEventId(1L);
        alert.setDescription("Test Alert");
        summary.setUnprocessedAlerts(Collections.singletonList(alert));
        
        TaskDTO task = new TaskDTO();
        task.setTaskId(1L);
        task.setTaskName("Current Task");
        summary.setCurrentTask(task);

        when(dashboardApplicationService.getSummary()).thenReturn(summary);

        mockMvc.perform(get("/api/dashboard/summary")
                .header("Authorization", "Bearer token"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.body.payload.code").value("RES-0000"))
                .andExpect(jsonPath("$.body.payload.data.inventoryTotal").value(1000))
                .andExpect(jsonPath("$.body.payload.data.todayAlertCount").value(5))
                .andExpect(jsonPath("$.body.payload.data.unprocessedAlerts[0].eventId").value(1))
                .andExpect(jsonPath("$.body.payload.data.currentTask.taskId").value(1));
    }
}
