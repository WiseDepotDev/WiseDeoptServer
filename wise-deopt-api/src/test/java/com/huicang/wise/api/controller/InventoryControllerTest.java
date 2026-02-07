package com.huicang.wise.api.controller;

import java.util.Collections;

import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.huicang.wise.api.config.JpaConfiguration;
import com.huicang.wise.application.inventory.InventoryApplicationService;
import com.huicang.wise.application.inventory.InventoryCreateRequest;
import com.huicang.wise.application.inventory.InventoryDTO;
import com.huicang.wise.application.inventory.InventoryReviewApplicationService;
import com.huicang.wise.application.inventory.ProductCreateRequest;
import com.huicang.wise.application.inventory.ProductDTO;
import com.huicang.wise.application.inventory.ProductUpdateRequest;

@WebMvcTest(controllers = InventoryController.class, excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JpaConfiguration.class))
public class InventoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private InventoryApplicationService inventoryApplicationService;

    @MockBean
    private InventoryReviewApplicationService inventoryReviewApplicationService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void testCreateProduct() throws Exception {
        ProductCreateRequest request = new ProductCreateRequest();
        request.setProductName("Test Product");
        request.setProductCode("CODE-001");
        request.setModel("Model-X");

        ProductDTO dto = new ProductDTO();
        dto.setProductId(1L);
        dto.setProductName("Test Product");
        dto.setProductCode("CODE-001");

        when(inventoryApplicationService.createProduct(any(ProductCreateRequest.class))).thenReturn(dto);

        mockMvc.perform(post("/api/inventories/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.body.payload.code").value("RES-0000"))
                .andExpect(jsonPath("$.body.payload.data.productId").value(1))
                .andExpect(jsonPath("$.body.payload.data.productName").value("Test Product"));
    }

    @Test
    void testUpdateProduct() throws Exception {
        Long productId = 1L;
        ProductUpdateRequest request = new ProductUpdateRequest();
        request.setProductName("Updated Product");

        ProductDTO dto = new ProductDTO();
        dto.setProductId(productId);
        dto.setProductName("Updated Product");

        when(inventoryApplicationService.updateProduct(eq(productId), any(ProductUpdateRequest.class))).thenReturn(dto);

        mockMvc.perform(put("/api/inventories/products/{productId}", productId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.body.payload.code").value("RES-0000"))
                .andExpect(jsonPath("$.body.payload.data.productName").value("Updated Product"));
    }

    @Test
    void testCreateInventory() throws Exception {
        InventoryCreateRequest request = new InventoryCreateRequest();
        request.setProductId(1L);
        request.setQuantity(100);
        request.setLocationCode("LOC-001");

        InventoryDTO dto = new InventoryDTO();
        dto.setInventoryId(1L);
        dto.setProductId(1L);
        dto.setQuantity(100);
        dto.setLocationCode("LOC-001");

        when(inventoryApplicationService.createInventory(any(InventoryCreateRequest.class))).thenReturn(dto);

        mockMvc.perform(post("/api/inventories")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.body.payload.code").value("RES-0000"))
                .andExpect(jsonPath("$.body.payload.data.inventoryId").value(1));
    }

    @Test
    void testListInventoryByProduct() throws Exception {
        Long productId = 1L;
        InventoryDTO dto = new InventoryDTO();
        dto.setInventoryId(1L);
        dto.setProductId(productId);

        when(inventoryApplicationService.listInventoryByProduct(productId)).thenReturn(Collections.singletonList(dto));

        mockMvc.perform(get("/api/inventories")
                .param("productId", productId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.body.payload.code").value("RES-0000"))
                .andExpect(jsonPath("$.body.payload.data[0].inventoryId").value(1));
    }
}
