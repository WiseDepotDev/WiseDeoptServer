package com.huicang.wise.application.inout;

import com.huicang.wise.common.exception.BusinessException;
import com.huicang.wise.infrastructure.repository.inout.StockOrderDetailJpaEntity;
import com.huicang.wise.infrastructure.repository.inout.StockOrderDetailRepository;
import com.huicang.wise.infrastructure.repository.inout.StockOrderJpaEntity;
import com.huicang.wise.infrastructure.repository.inout.StockOrderRepository;
import com.huicang.wise.infrastructure.repository.inventory.InventoryJpaEntity;
import com.huicang.wise.infrastructure.repository.inventory.InventoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InOutApplicationServiceTest {

    @Mock
    private StockOrderRepository stockOrderRepository;

    @Mock
    private StockOrderDetailRepository stockOrderDetailRepository;

    @Mock
    private InventoryRepository inventoryRepository;

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private InOutApplicationService inOutApplicationService;

    @BeforeEach
    void setUp() {
        lenient().when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    void testCreateStockOrder() {
        StockOrderCreateRequest request = new StockOrderCreateRequest();
        request.setOrderNo("IN-001");
        request.setOrderType("INBOUND");
        
        StockOrderItemDTO item = new StockOrderItemDTO();
        item.setProductId(1L);
        item.setQuantity(10);
        item.setLocationCode("A-01");
        request.setItems(Collections.singletonList(item));

        when(stockOrderRepository.save(any(StockOrderJpaEntity.class))).thenAnswer(invocation -> {
            StockOrderJpaEntity entity = invocation.getArgument(0);
            entity.setOrderId(100L);
            return entity;
        });

        StockOrderDTO result = inOutApplicationService.createStockOrder(request);

        assertNotNull(result);
        assertEquals("IN-001", result.getOrderNo());
        verify(stockOrderRepository, times(1)).save(any(StockOrderJpaEntity.class));
        verify(stockOrderDetailRepository, times(1)).save(any(StockOrderDetailJpaEntity.class));
    }

    @Test
    void testSubmitStockOrderInbound() {
        Long orderId = 100L;
        StockOrderJpaEntity order = new StockOrderJpaEntity();
        order.setOrderId(orderId);
        order.setOrderType("INBOUND");
        order.setOrderStatus("CREATED");

        StockOrderDetailJpaEntity detail = new StockOrderDetailJpaEntity();
        detail.setProductId(1L);
        detail.setQuantity(10);
        detail.setLocationCode("A-01");

        when(stockOrderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(stockOrderDetailRepository.findByOrderId(orderId)).thenReturn(Collections.singletonList(detail));
        when(inventoryRepository.findByProductIdAndLocationCode(1L, "A-01")).thenReturn(Optional.empty());
        when(stockOrderRepository.save(any(StockOrderJpaEntity.class))).thenReturn(order);

        inOutApplicationService.submitStockOrder(orderId);

        verify(inventoryRepository, times(1)).save(any(InventoryJpaEntity.class));
        assertEquals("SUBMITTED", order.getOrderStatus());
    }

    @Test
    void testSubmitStockOrderOutboundSuccess() {
        Long orderId = 101L;
        StockOrderJpaEntity order = new StockOrderJpaEntity();
        order.setOrderId(orderId);
        order.setOrderType("OUTBOUND");
        order.setOrderStatus("CREATED");

        StockOrderDetailJpaEntity detail = new StockOrderDetailJpaEntity();
        detail.setProductId(1L);
        detail.setQuantity(5);
        detail.setLocationCode("A-01");

        InventoryJpaEntity inventory = new InventoryJpaEntity();
        inventory.setProductId(1L);
        inventory.setQuantity(10);
        inventory.setLocationCode("A-01");

        when(stockOrderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(stockOrderDetailRepository.findByOrderId(orderId)).thenReturn(Collections.singletonList(detail));
        when(inventoryRepository.findByProductIdAndLocationCode(1L, "A-01")).thenReturn(Optional.of(inventory));
        when(stockOrderRepository.save(any(StockOrderJpaEntity.class))).thenReturn(order);

        inOutApplicationService.submitStockOrder(orderId);

        assertEquals(5, inventory.getQuantity());
        assertEquals("SUBMITTED", order.getOrderStatus());
    }

    @Test
    void testSubmitStockOrderOutboundInsufficient() {
        Long orderId = 102L;
        StockOrderJpaEntity order = new StockOrderJpaEntity();
        order.setOrderId(orderId);
        order.setOrderType("OUTBOUND");
        order.setOrderStatus("CREATED");

        StockOrderDetailJpaEntity detail = new StockOrderDetailJpaEntity();
        detail.setProductId(1L);
        detail.setQuantity(15); // Need 15
        detail.setLocationCode("A-01");

        InventoryJpaEntity inventory = new InventoryJpaEntity();
        inventory.setProductId(1L);
        inventory.setQuantity(10); // Have 10
        inventory.setLocationCode("A-01");

        when(stockOrderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(stockOrderDetailRepository.findByOrderId(orderId)).thenReturn(Collections.singletonList(detail));
        when(inventoryRepository.findByProductIdAndLocationCode(1L, "A-01")).thenReturn(Optional.of(inventory));

        assertThrows(BusinessException.class, () -> inOutApplicationService.submitStockOrder(orderId));
    }
}
