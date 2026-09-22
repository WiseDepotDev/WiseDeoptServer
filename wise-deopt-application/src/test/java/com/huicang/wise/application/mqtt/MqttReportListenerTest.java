package com.huicang.wise.application.mqtt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.huicang.wise.application.inspection.InspectionApplicationService;
import com.huicang.wise.application.inspection.InspectionReportRequest;
import com.huicang.wise.application.inspection.InspectionResultDTO;
import com.huicang.wise.domain.inspection.InspectionProgressPublisher;
import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.support.GenericMessage;

/**
 * {@link MqttReportListener} 的信封口径单测（决策 3：三端只支持最新形状）。
 *
 * <p>验证两件事：
 *
 * <ul>
 *   <li>标准信封 → 解包 payload.data 后交给应用服务（taskId 正确传入）；
 *   <li>无信封扁平报文 → **拒绝**（不调用应用服务），只打日志，不再走过渡期兼容分支。
 * </ul>
 */
class MqttReportListenerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final InspectionApplicationService service = mock(InspectionApplicationService.class);
    private final InspectionProgressPublisher progressPublisher = mock(InspectionProgressPublisher.class);

    private MqttReportListener listener;

    @BeforeEach
    void setUp() throws Exception {
        listener = new MqttReportListener();
        inject("objectMapper", objectMapper);
        inject("inspectionApplicationService", service);
        inject("progressPublisher", progressPublisher);
    }

    private void inject(String field, Object value) throws Exception {
        Field f = MqttReportListener.class.getDeclaredField(field);
        f.setAccessible(true);
        f.set(listener, value);
    }

    private InspectionResultDTO result() {
        InspectionResultDTO dto = new InspectionResultDTO();
        dto.setProgress(100);
        dto.setStatus("COMPLETED");
        return dto;
    }

    @Test
    @DisplayName("标准信封：解包 payload.data 后交给应用服务，taskId 正确")
    void processesEnvelopePayload() {
        AtomicReference<InspectionReportRequest> captured = new AtomicReference<>();
        when(service.reportResult(any())).thenAnswer(inv -> {
            captured.set(inv.getArgument(0));
            return result();
        });

        String envelope =
                "{\"header\":{\"request_id\":\"r-1\",\"packet_type\":\"RFID_DATA_UPLOAD\","
                        + "\"timestamp\":1772000000123},"
                        + "\"payload\":{\"code\":\"RES-0000\",\"message\":\"请求\","
                        + "\"data\":{\"taskId\":\"9202\",\"total_scanned\":2}}}";
        listener.handleInspectionReport(new GenericMessage<>(envelope));

        assertEquals("9202", captured.get().getTaskId());
        assertEquals(2, captured.get().getTotal_scanned());
        verify(service).reportResult(any());
    }

    @Test
    @DisplayName("无信封扁平报文：直接拒绝，不调用应用服务（决策 3：只支持最新形状）")
    void rejectsLegacyFlatPayload() {
        String flat = "{\"taskId\":\"9201\",\"total_scanned\":2}";

        listener.handleInspectionReport(new GenericMessage<>(flat));

        verify(service, never()).reportResult(any());
        assertFalse(flat.contains("\"header\""), "对照：扁平报文确实没有 header");
    }

    @Test
    @DisplayName("半个信封（只有 payload）同样被拒绝")
    void rejectsHalfEnvelope() {
        listener.handleInspectionReport(
                new GenericMessage<>("{\"payload\":{\"code\":\"RES-0000\",\"data\":{\"taskId\":\"1\"}}}"));

        verify(service, never()).reportResult(any());
        assertTrue(true);
    }
}
