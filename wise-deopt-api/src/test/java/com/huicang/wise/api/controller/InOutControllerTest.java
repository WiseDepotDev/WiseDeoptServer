package com.huicang.wise.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.huicang.wise.application.inout.InOutApplicationService;
import com.huicang.wise.application.inout.StockOrderCreateRequest;
import com.huicang.wise.application.inout.StockOrderDTO;
import com.huicang.wise.application.inout.StockOrderPageDTO;
import com.huicang.wise.api.config.JpaConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = InOutController.class, excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JpaConfiguration.class))
public class InOutControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private InOutApplicationService inOutApplicationService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void testCreateStockOrder() throws Exception {
        StockOrderCreateRequest request = new StockOrderCreateRequest();
        request.setOrderNo("IN-001");
        request.setOrderType("INBOUND");

        StockOrderDTO dto = new StockOrderDTO();
        dto.setOrderId(1L);
        dto.setOrderNo("IN-001");
        dto.setOrderType("INBOUND");
        dto.setOrderStatus("CREATED");
        dto.setCreatedAt(LocalDateTime.now());

        when(inOutApplicationService.createStockOrder(any(StockOrderCreateRequest.class))).thenReturn(dto);

        mockMvc.perform(post("/api/stock-orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.body.payload.code").value("RES-0000"))
                .andExpect(jsonPath("$.body.payload.data.orderId").value(1))
                .andExpect(jsonPath("$.body.payload.data.orderNo").value("IN-001"));
    }

    @Test
    void testListStockOrders() throws Exception {
        StockOrderPageDTO pageDTO = new StockOrderPageDTO();
        StockOrderDTO dto = new StockOrderDTO();
        dto.setOrderId(1L);
        dto.setOrderNo("IN-001");
        pageDTO.setRows(Collections.singletonList(dto));
        pageDTO.setTotal(1L);

        when(inOutApplicationService.listStockOrders(any(), any())).thenReturn(pageDTO);

        mockMvc.perform(get("/api/stock-orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.body.payload.code").value("RES-0000"))
                .andExpect(jsonPath("$.body.payload.data.rows[0].orderId").value(1));
    }

    @Test
    void testSubmitStockOrder() throws Exception {
        Long orderId = 1L;
        StockOrderDTO dto = new StockOrderDTO();
        dto.setOrderId(orderId);
        dto.setOrderStatus("SUBMITTED");

        when(inOutApplicationService.submitStockOrder(orderId)).thenReturn(dto);

        mockMvc.perform(post("/api/stock-orders/{orderId}/submit", orderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.body.payload.code").value("RES-0000"))
                .andExpect(jsonPath("$.body.payload.data.orderStatus").value("SUBMITTED"));
    }

    @Test
    void testGetStockOrder() throws Exception {
        Long orderId = 1L;
        StockOrderDTO dto = new StockOrderDTO();
        dto.setOrderId(orderId);
        dto.setOrderNo("IN-001");

        when(inOutApplicationService.getStockOrder(orderId)).thenReturn(dto);

        mockMvc.perform(get("/api/stock-orders/{orderId}", orderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.body.payload.code").value("RES-0000"))
                .andExpect(jsonPath("$.body.payload.data.orderId").value(1));
    }
}
