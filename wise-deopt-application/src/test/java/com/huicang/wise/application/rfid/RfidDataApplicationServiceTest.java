package com.huicang.wise.application.rfid;

import com.huicang.wise.application.alert.AlertApplicationService;
import com.huicang.wise.application.alert.AlertCreateRequest;
import com.huicang.wise.infrastructure.repository.device.DeviceCoreJpaEntity;
import com.huicang.wise.infrastructure.repository.device.DeviceCoreRepository;
import com.huicang.wise.infrastructure.repository.inout.StockOrderDetailRepository;
import com.huicang.wise.infrastructure.repository.inout.StockOrderRepository;
import com.huicang.wise.infrastructure.repository.tag.ProductTagJpaEntity;
import com.huicang.wise.infrastructure.repository.tag.ProductTagRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RfidDataApplicationServiceTest {

    @Mock
    private DeviceCoreRepository deviceCoreRepository;
    @Mock
    private ProductTagRepository productTagRepository;
    @Mock
    private StockOrderRepository stockOrderRepository;
    @Mock
    private StockOrderDetailRepository stockOrderDetailRepository;
    @Mock
    private AlertApplicationService alertApplicationService;

    private RfidDataApplicationService rfidDataApplicationService;

    @BeforeEach
    void setUp() {
        rfidDataApplicationService = new RfidDataApplicationService(
                deviceCoreRepository,
                productTagRepository,
                stockOrderRepository,
                stockOrderDetailRepository,
                alertApplicationService
        );
    }

    @Test
    void processRfidReport_ShouldTriggerAlert_WhenUnauthorizedMovementDetected() {
        // Arrange
        Long deviceId = 123L;
        String rfid = "rfid-123";
        String snapshotUrl = "http://example.com/snapshot.jpg";
        Long productId = 100L;

        RfidReportDTO request = new RfidReportDTO();
        request.setDeviceId(deviceId);
        request.setRfidTags(Collections.singletonList(rfid));
        request.setSnapshotUrl(snapshotUrl);

        DeviceCoreJpaEntity device = new DeviceCoreJpaEntity();
        device.setDeviceId(deviceId);
        device.setDeviceCode("DEVICE_CODE_123");
        device.setName("Test Reader");
        device.setType(0); // RFID Reader

        ProductTagJpaEntity tag = new ProductTagJpaEntity();
        tag.setTagId(1L);
        tag.setRfid(rfid);
        tag.setProductId(productId);

        when(deviceCoreRepository.findById(deviceId)).thenReturn(Optional.of(device));
        when(productTagRepository.findByRfid(rfid)).thenReturn(tag);
        // Simulate no active order for this product
        when(stockOrderDetailRepository.findByProductId(productId)).thenReturn(Collections.emptyList());

        // Act
        rfidDataApplicationService.processRfidReport(request);

        // Assert
        ArgumentCaptor<AlertCreateRequest> captor = ArgumentCaptor.forClass(AlertCreateRequest.class);
        verify(alertApplicationService).createAlert(captor.capture());

        AlertCreateRequest capturedRequest = captor.getValue();
        assertEquals("UNAUTHORIZED_MOVEMENT", capturedRequest.getAlertType());
        assertEquals("HIGH", capturedRequest.getAlertLevel());
        assertTrue(capturedRequest.getDescription().contains(device.getName()));
        assertTrue(capturedRequest.getDescription().contains(rfid));
        assertTrue(capturedRequest.getDescription().contains(snapshotUrl));
    }

    @Test
    void processRfidReport_ShouldNotTriggerAlert_WhenActiveOrderExists() {
        // Arrange
        Long deviceId = 123L;
        String rfid = "rfid-123";
        Long productId = 100L;
        Long orderId = 888L;

        RfidReportDTO request = new RfidReportDTO();
        request.setDeviceId(deviceId);
        request.setRfidTags(Collections.singletonList(rfid));

        DeviceCoreJpaEntity device = new DeviceCoreJpaEntity();
        device.setDeviceId(deviceId);
        device.setDeviceCode("DEVICE_CODE_123");
        device.setType(0);

        ProductTagJpaEntity tag = new ProductTagJpaEntity();
        tag.setTagId(1L);
        tag.setRfid(rfid);
        tag.setProductId(productId);

        com.huicang.wise.infrastructure.repository.inout.StockOrderDetailJpaEntity detail = new com.huicang.wise.infrastructure.repository.inout.StockOrderDetailJpaEntity();
        detail.setOrderId(orderId);
        detail.setProductId(productId);

        com.huicang.wise.infrastructure.repository.inout.StockOrderJpaEntity order = new com.huicang.wise.infrastructure.repository.inout.StockOrderJpaEntity();
        order.setOrderId(orderId);
        order.setOrderStatus("PENDING"); // Not SUBMITTED

        when(deviceCoreRepository.findById(deviceId)).thenReturn(Optional.of(device));
        when(productTagRepository.findByRfid(rfid)).thenReturn(tag);
        when(stockOrderDetailRepository.findByProductId(productId)).thenReturn(Collections.singletonList(detail));
        when(stockOrderRepository.findById(orderId)).thenReturn(Optional.of(order));

        // Act
        rfidDataApplicationService.processRfidReport(request);

        // Assert
        verify(alertApplicationService, never()).createAlert(any());
    }
}
