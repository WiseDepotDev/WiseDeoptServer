package com.huicang.wise.application.rfid;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.huicang.wise.application.alert.AlertApplicationService;
import com.huicang.wise.application.alert.AlertCreateRequest;
import com.huicang.wise.application.alert.AlertDTO;
import com.huicang.wise.application.message.MessageApplicationService;
import com.huicang.wise.application.message.MessageCreateRequest;
import com.huicang.wise.application.message.MessageDTO;
import com.huicang.wise.common.api.ErrorCode;
import com.huicang.wise.common.exception.BusinessException;
import com.huicang.wise.domain.alert.AlertEvent;
import com.huicang.wise.domain.device.DeviceCore;
import com.huicang.wise.domain.inout.StockOrderDetail;
import com.huicang.wise.domain.tag.ProductTag;
import com.huicang.wise.domain.user.UserCore;
import com.huicang.wise.infrastructure.persistence.repository.alert.AlertEventRepository;
import com.huicang.wise.infrastructure.persistence.repository.device.DeviceRepository;
import com.huicang.wise.infrastructure.persistence.repository.inout.StockOrderDetailRepository;
import com.huicang.wise.infrastructure.persistence.repository.inout.StockOrderRepository;
import com.huicang.wise.infrastructure.persistence.repository.tag.ProductTagRepository;
import com.huicang.wise.infrastructure.persistence.repository.user.UserRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * RFID 上报应用服务的单元测试：入参校验与短路、去重、设备校验、以及"是否需要告警"的三道判定 （未解决告警抑制 → 有效出库单豁免 → 才触发告警），最后是告警与消息两条旁路的失败隔离。
 *
 * <p>本批钉住的语义： ① {@code rfidTags} 为空时**在设备校验之前**就直接返回（不是报错）； ② RFID 先**去重**再逐个处理； ③ 只有 `device.type
 * == 0` 才进入告警判定，且判定顺序是"已有未解决告警 → 跳过"优先于"查有效出库单"； ④ 告警与消息是**旁路**：任一步失败都只留痕、不影响主流程（STD-ERR-02）。
 */
@ExtendWith(MockitoExtension.class)
class RfidDataApplicationServiceTest {

    private static final long DEVICE_ID = 7L;
    private static final long TAG_ID = 21L;
    private static final long PRODUCT_ID = 33L;
    private static final String RFID = "RFID-1";

    @Mock private DeviceRepository deviceRepository;
    @Mock private ProductTagRepository productTagRepository;
    @Mock private StockOrderRepository stockOrderRepository;
    @Mock private StockOrderDetailRepository stockOrderDetailRepository;
    @Mock private AlertApplicationService alertApplicationService;
    @Mock private MessageApplicationService messageApplicationService;
    @Mock private UserRepository userRepository;
    @Mock private AlertEventRepository alertEventRepository;

    private RfidDataApplicationService service;

    @BeforeEach
    void setUp() {
        service =
                new RfidDataApplicationService(
                        deviceRepository,
                        productTagRepository,
                        stockOrderRepository,
                        stockOrderDetailRepository,
                        alertApplicationService,
                        messageApplicationService,
                        userRepository,
                        alertEventRepository);
    }

    private RfidReportDTO report(List<String> tags) {
        RfidReportDTO request = new RfidReportDTO();
        request.setDeviceId(DEVICE_ID);
        request.setRfidTags(tags);
        return request;
    }

    private DeviceCore device(short type) {
        DeviceCore entity = new DeviceCore();
        entity.setType(type);
        entity.setName("门禁A");
        entity.setDeviceCode("DEV-001");
        return entity;
    }

    private ProductTag tag() {
        ProductTag entity = new ProductTag();
        entity.setTagId(TAG_ID);
        entity.setProductId(PRODUCT_ID);
        entity.setRfid(RFID);
        return entity;
    }

    private AlertEvent unresolvedAlert() {
        AlertEvent entity = new AlertEvent();
        entity.setStatus((short) 0);
        entity.setMessage("历史告警 " + RFID);
        return entity;
    }

    private void stubNoUnresolvedAlert() {
        when(alertEventRepository.findByStatusAndMessageContainingRfid(0, RFID))
                .thenReturn(List.of());
    }

    private AlertCreateRequest capturedAlertRequest() {
        ArgumentCaptor<AlertCreateRequest> captor =
                ArgumentCaptor.forClass(AlertCreateRequest.class);
        verify(alertApplicationService).createAlert(captor.capture());
        return captor.getValue();
    }

    private void stubAlertCreation() {
        when(alertApplicationService.createAlert(any(AlertCreateRequest.class)))
                .thenReturn(new AlertDTO());
    }

    // ---------------- 入参与短路 ----------------

    @Test
    @DisplayName("上报：设备ID为空抛 PARAM_ERROR")
    void rejectsNullDeviceId() {
        RfidReportDTO request = new RfidReportDTO();
        request.setRfidTags(List.of(RFID));

        BusinessException ex =
                assertThrows(BusinessException.class, () -> service.processRfidReport(request));

        assertEquals(ErrorCode.PARAM_ERROR, ex.getErrorCode());
        verifyNoInteractions(deviceRepository);
    }

    @Test
    @DisplayName("上报：标签列表为 null 时直接返回（不校验设备）")
    void nullTagsShortCircuits() {
        service.processRfidReport(report(null));

        verifyNoInteractions(deviceRepository);
    }

    @Test
    @DisplayName("上报：标签列表为空时直接返回（不校验设备）")
    void emptyTagsShortCircuits() {
        service.processRfidReport(report(List.of()));

        verifyNoInteractions(deviceRepository);
    }

    @Test
    @DisplayName("上报：设备不存在抛 PARAM_ERROR")
    void rejectsUnknownDevice() {
        when(deviceRepository.findById(DEVICE_ID)).thenReturn(Optional.empty());

        BusinessException ex =
                assertThrows(
                        BusinessException.class,
                        () -> service.processRfidReport(report(List.of(RFID))));

        assertEquals(ErrorCode.PARAM_ERROR, ex.getErrorCode());
        verifyNoInteractions(productTagRepository);
    }

    @Test
    @DisplayName("上报：重复 RFID 先去重，同一标签只处理一次")
    void distinctsRfidTags() {
        when(deviceRepository.findById(DEVICE_ID)).thenReturn(Optional.of(device((short) 1)));
        when(productTagRepository.findByRfid(anyString())).thenReturn(Optional.empty());

        service.processRfidReport(report(List.of("A", "A", "B")));

        verify(productTagRepository, times(2)).findByRfid(anyString());
    }

    // ---------------- 告警判定 ----------------

    @Test
    @DisplayName("标签未登记：不告警")
    void unknownTagDoesNotAlert() {
        when(deviceRepository.findById(DEVICE_ID)).thenReturn(Optional.of(device((short) 0)));
        when(productTagRepository.findByRfid(RFID)).thenReturn(Optional.empty());

        service.processRfidReport(report(List.of(RFID)));

        verifyNoInteractions(alertApplicationService);
    }

    @Test
    @DisplayName("非门禁类设备（type != 0）：不进入告警判定")
    void nonGateDeviceSkipsAlertLogic() {
        when(deviceRepository.findById(DEVICE_ID)).thenReturn(Optional.of(device((short) 1)));
        when(productTagRepository.findByRfid(RFID)).thenReturn(Optional.of(tag()));

        service.processRfidReport(report(List.of(RFID)));

        verifyNoInteractions(alertApplicationService);
        verifyNoInteractions(alertEventRepository);
    }

    @Test
    @DisplayName("已有未解决告警：跳过，且不再查有效出库单（判定顺序）")
    void unresolvedAlertSuppressesAndSkipsOutboundCheck() {
        when(deviceRepository.findById(DEVICE_ID)).thenReturn(Optional.of(device((short) 0)));
        when(productTagRepository.findByRfid(RFID)).thenReturn(Optional.of(tag()));
        when(alertEventRepository.findByStatusAndMessageContainingRfid(0, RFID))
                .thenReturn(List.of(unresolvedAlert()));

        service.processRfidReport(report(List.of(RFID)));

        verifyNoInteractions(alertApplicationService);
        verify(stockOrderDetailRepository, never()).findValidOutboundOrderDetailByTagId(any());
    }

    @Test
    @DisplayName("有有效出库单据：豁免告警")
    void validOutboundOrderExempts() {
        when(deviceRepository.findById(DEVICE_ID)).thenReturn(Optional.of(device((short) 0)));
        when(productTagRepository.findByRfid(RFID)).thenReturn(Optional.of(tag()));
        stubNoUnresolvedAlert();
        when(stockOrderDetailRepository.findValidOutboundOrderDetailByTagId(TAG_ID))
                .thenReturn(Optional.of(new StockOrderDetail()));

        service.processRfidReport(report(List.of(RFID)));

        verifyNoInteractions(alertApplicationService);
    }

    @Test
    @DisplayName("无有效出库单据：触发 3 级违规移动告警，描述含设备与产品信息")
    void triggersUnauthorizedMovementAlert() {
        when(deviceRepository.findById(DEVICE_ID)).thenReturn(Optional.of(device((short) 0)));
        when(productTagRepository.findByRfid(RFID)).thenReturn(Optional.of(tag()));
        stubNoUnresolvedAlert();
        when(stockOrderDetailRepository.findValidOutboundOrderDetailByTagId(TAG_ID))
                .thenReturn(Optional.empty());
        stubAlertCreation();

        service.processRfidReport(report(List.of(RFID)));

        AlertCreateRequest alert = capturedAlertRequest();
        assertEquals("UNAUTHORIZED_MOVEMENT", alert.getAlertType());
        assertEquals("RFID", alert.getSourceModule());
        assertEquals("3", alert.getAlertLevel());
        assertTrue(alert.getDescription().contains("门禁A"), alert.getDescription());
        assertTrue(alert.getDescription().contains("DEV-001"), alert.getDescription());
        assertTrue(alert.getDescription().contains(RFID), alert.getDescription());
        assertTrue(
                alert.getDescription().contains(String.valueOf(PRODUCT_ID)),
                alert.getDescription());
        assertNull(alert.getSnapshotUrl());
    }

    @Test
    @DisplayName("带抓拍地址：描述包含抓拍并可回填到请求")
    void includesSnapshotUrlWhenPresent() {
        when(deviceRepository.findById(DEVICE_ID)).thenReturn(Optional.of(device((short) 0)));
        when(productTagRepository.findByRfid(RFID)).thenReturn(Optional.of(tag()));
        stubNoUnresolvedAlert();
        when(stockOrderDetailRepository.findValidOutboundOrderDetailByTagId(TAG_ID))
                .thenReturn(Optional.empty());
        stubAlertCreation();
        RfidReportDTO request = report(List.of(RFID));
        request.setSnapshotUrl("http://snap/1.png");

        service.processRfidReport(request);

        AlertCreateRequest alert = capturedAlertRequest();
        assertTrue(alert.getDescription().contains("http://snap/1.png"), alert.getDescription());
    }

    @Test
    @DisplayName("空抓拍地址：描述不含抓拍段")
    void ignoresEmptySnapshotUrl() {
        when(deviceRepository.findById(DEVICE_ID)).thenReturn(Optional.of(device((short) 0)));
        when(productTagRepository.findByRfid(RFID)).thenReturn(Optional.of(tag()));
        stubNoUnresolvedAlert();
        when(stockOrderDetailRepository.findValidOutboundOrderDetailByTagId(TAG_ID))
                .thenReturn(Optional.empty());
        stubAlertCreation();
        RfidReportDTO request = report(List.of(RFID));
        request.setSnapshotUrl("");

        service.processRfidReport(request);

        assertNull(capturedAlertRequest().getSnapshotUrl());
    }

    // ---------------- 旁路失败隔离 ----------------

    @Test
    @DisplayName("告警创建失败被吞掉：不影响上报主流程（STD-ERR-02）")
    void alertCreationFailureIsSwallowed() {
        when(deviceRepository.findById(DEVICE_ID)).thenReturn(Optional.of(device((short) 0)));
        when(productTagRepository.findByRfid(RFID)).thenReturn(Optional.of(tag()));
        stubNoUnresolvedAlert();
        when(stockOrderDetailRepository.findValidOutboundOrderDetailByTagId(TAG_ID))
                .thenReturn(Optional.empty());
        when(alertApplicationService.createAlert(any(AlertCreateRequest.class)))
                .thenThrow(new BusinessException(ErrorCode.SYSTEM_ERROR, "告警服务不可用"));

        service.processRfidReport(report(List.of(RFID)));

        verify(messageApplicationService, never()).createMessage(any());
    }

    @Test
    @DisplayName("告警成功后给全部用户各发一条告警消息并推送")
    void sendsMessageToAllUsers() {
        when(deviceRepository.findById(DEVICE_ID)).thenReturn(Optional.of(device((short) 0)));
        when(productTagRepository.findByRfid(RFID)).thenReturn(Optional.of(tag()));
        stubNoUnresolvedAlert();
        when(stockOrderDetailRepository.findValidOutboundOrderDetailByTagId(TAG_ID))
                .thenReturn(Optional.empty());
        stubAlertCreation();
        UserCore u1 = new UserCore();
        u1.setUserId(1L);
        UserCore u2 = new UserCore();
        u2.setUserId(2L);
        when(userRepository.findAll()).thenReturn(List.of(u1, u2));
        when(messageApplicationService.createMessage(any(MessageCreateRequest.class)))
                .thenReturn(new MessageDTO());

        service.processRfidReport(report(List.of(RFID)));

        ArgumentCaptor<MessageCreateRequest> captor =
                ArgumentCaptor.forClass(MessageCreateRequest.class);
        verify(messageApplicationService, times(2)).createMessage(captor.capture());
        assertEquals("违规移动告警", captor.getAllValues().get(0).getTitle());
        assertEquals(1, captor.getAllValues().get(0).getPriority());
        verify(messageApplicationService, times(2)).sendPushNotification(any());
    }

    @Test
    @DisplayName("消息发送失败被吞掉：不影响主流程")
    void messageFailureIsSwallowed() {
        when(deviceRepository.findById(DEVICE_ID)).thenReturn(Optional.of(device((short) 0)));
        when(productTagRepository.findByRfid(RFID)).thenReturn(Optional.of(tag()));
        stubNoUnresolvedAlert();
        when(stockOrderDetailRepository.findValidOutboundOrderDetailByTagId(TAG_ID))
                .thenReturn(Optional.empty());
        stubAlertCreation();
        when(userRepository.findAll()).thenReturn(List.of(new UserCore()));
        doThrow(new RuntimeException("消息服务不可用"))
                .when(messageApplicationService)
                .createMessage(any(MessageCreateRequest.class));

        service.processRfidReport(report(List.of(RFID)));

        verify(alertApplicationService).createAlert(any(AlertCreateRequest.class));
    }

    @Test
    @DisplayName("没有用户时不发送任何消息")
    void noUsersNoMessages() {
        when(deviceRepository.findById(DEVICE_ID)).thenReturn(Optional.of(device((short) 0)));
        when(productTagRepository.findByRfid(RFID)).thenReturn(Optional.of(tag()));
        stubNoUnresolvedAlert();
        when(stockOrderDetailRepository.findValidOutboundOrderDetailByTagId(TAG_ID))
                .thenReturn(Optional.empty());
        stubAlertCreation();
        when(userRepository.findAll()).thenReturn(List.of());

        service.processRfidReport(report(List.of(RFID)));

        verify(messageApplicationService, never()).createMessage(any());
    }
}
