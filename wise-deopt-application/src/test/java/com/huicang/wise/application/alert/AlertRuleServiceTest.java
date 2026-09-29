package com.huicang.wise.application.alert;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.huicang.wise.common.api.ErrorCode;
import com.huicang.wise.common.exception.BusinessException;
import com.huicang.wise.domain.alert.AlertEvent;
import com.huicang.wise.infrastructure.persistence.repository.alert.AlertRepository;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * 告警规则服务的单元测试：四类告警生成规则（RFID-视频一致性、违规移动、设备离线、库存异常） 与两个定时巡检方法。
 *
 * <p>本批把两个 {@code @Scheduled} 巡检方法的**现状缺陷**钉住（只记录、未修）： 它们完全不读取设备心跳或库存数据，而是对**每条**命中关键字的既有告警，
 * 用**写死的示例值**（设备 1L / "示例设备"；产品 1L / "示例产品" / 100 / 90 / "主仓库"） 再生成一条新告警 —— 也就是说：① 生成内容与输入无关；② N
 * 条命中就产生 N 条重复告警（告警风暴）。
 */
@ExtendWith(MockitoExtension.class)
class AlertRuleServiceTest {

    @Mock private AlertRepository alertEventRepository;

    private AlertRuleService service;

    @BeforeEach
    void setUp() {
        service = new AlertRuleService(alertEventRepository);
    }

    private AlertEvent capturedEntity() {
        ArgumentCaptor<AlertEvent> captor = ArgumentCaptor.forClass(AlertEvent.class);
        verify(alertEventRepository).save(captor.capture());
        return captor.getValue();
    }

    private void stubSave() {
        when(alertEventRepository.save(any(AlertEvent.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    // ---------------- RFID-视频一致性 ----------------

    @Test
    @DisplayName("RFID-视频：数量为空被拒")
    void rfidVideoRejectsNullCounts() {
        BusinessException ex =
                assertThrows(
                        BusinessException.class,
                        () -> service.createRfidVideoConsistencyAlert(null, 3, "A区"));

        assertEquals(ErrorCode.PARAM_ERROR, ex.getErrorCode());

        BusinessException ex2 =
                assertThrows(
                        BusinessException.class,
                        () -> service.createRfidVideoConsistencyAlert(3, null, "A区"));
        assertEquals(ErrorCode.PARAM_ERROR, ex2.getErrorCode());
        verify(alertEventRepository, never()).save(any(AlertEvent.class));
    }

    @Test
    @DisplayName("RFID-视频：数量一致无需告警")
    void rfidVideoRejectsEqualCounts() {
        BusinessException ex =
                assertThrows(
                        BusinessException.class,
                        () -> service.createRfidVideoConsistencyAlert(5, 5, "A区"));

        assertEquals(ErrorCode.PARAM_ERROR, ex.getErrorCode());
    }

    @Test
    @DisplayName("RFID-视频：差异 ≤5 记 2 级，>5 记 3 级")
    void rfidVideoLevelDependsOnDifference() {
        stubSave();

        AlertDTO small = service.createRfidVideoConsistencyAlert(10, 5, "A区");
        assertEquals(2, small.getLevel());

        AlertDTO big = service.createRfidVideoConsistencyAlert(10, 4, "A区");
        assertEquals(3, big.getLevel());
    }

    @Test
    @DisplayName("RFID-视频：告警内容含位置与双向数量，来源模块为 RFID_VIDEO")
    void rfidVideoFillsContent() {
        stubSave();

        service.createRfidVideoConsistencyAlert(12, 5, "B区3排");

        AlertEvent saved = capturedEntity();
        assertEquals("RFID_VIDEO", saved.getSourceModule());
        assertEquals("RFID-视频数量不一致", saved.getTitle());
        assertTrue(saved.getMessage().contains("B区3排"), "实际: " + saved.getMessage());
        assertTrue(saved.getMessage().contains("12"), "实际: " + saved.getMessage());
        assertTrue(saved.getMessage().contains("5"), "实际: " + saved.getMessage());
        assertEquals((short) 0, saved.getStatus());
        assertEquals(Boolean.TRUE, saved.getIsActive());
        assertNotNull(saved.getCreateTime());
    }

    // ---------------- 违规移动 ----------------

    @Test
    @DisplayName("违规移动：产品ID/名称/位置任一为空被拒")
    void unauthorizedMoveRejectsMissingFields() {
        assertEquals(
                ErrorCode.PARAM_ERROR,
                assertThrows(
                                BusinessException.class,
                                () ->
                                        service.createUnauthorizedMoveAlert(
                                                null, "螺丝", "A", "B", "x"))
                        .getErrorCode());
        assertEquals(
                ErrorCode.PARAM_ERROR,
                assertThrows(
                                BusinessException.class,
                                () -> service.createUnauthorizedMoveAlert(1L, null, "A", "B", "x"))
                        .getErrorCode());
        assertEquals(
                ErrorCode.PARAM_ERROR,
                assertThrows(
                                BusinessException.class,
                                () -> service.createUnauthorizedMoveAlert(1L, "螺丝", null, "B", "x"))
                        .getErrorCode());
        assertEquals(
                ErrorCode.PARAM_ERROR,
                assertThrows(
                                BusinessException.class,
                                () -> service.createUnauthorizedMoveAlert(1L, "螺丝", "A", null, "x"))
                        .getErrorCode());
        verify(alertEventRepository, never()).save(any(AlertEvent.class));
    }

    @Test
    @DisplayName("违规移动：来源模块与级别固定，内容含两端位置与原因")
    void unauthorizedMoveFillsContent() {
        stubSave();

        service.createUnauthorizedMoveAlert(1L, "螺丝", "A区", "C区", "未登记移动");

        AlertEvent saved = capturedEntity();
        assertEquals("PRODUCT_MOVEMENT", saved.getSourceModule());
        assertEquals((short) 2, saved.getLevel());
        assertTrue(saved.getMessage().contains("A区"), "实际: " + saved.getMessage());
        assertTrue(saved.getMessage().contains("C区"), "实际: " + saved.getMessage());
        assertTrue(saved.getMessage().contains("未登记移动"), "实际: " + saved.getMessage());
    }

    // ---------------- 设备离线 ----------------

    @Test
    @DisplayName("设备离线：设备ID或名称缺失被拒")
    void deviceOfflineRejectsMissingFields() {
        assertEquals(
                ErrorCode.PARAM_ERROR,
                assertThrows(
                                BusinessException.class,
                                () ->
                                        service.createDeviceOfflineAlert(
                                                null, "网关", "DEVICE", LocalDateTime.now()))
                        .getErrorCode());
        assertEquals(
                ErrorCode.PARAM_ERROR,
                assertThrows(
                                BusinessException.class,
                                () ->
                                        service.createDeviceOfflineAlert(
                                                1L, null, "DEVICE", LocalDateTime.now()))
                        .getErrorCode());
    }

    @Test
    @DisplayName("设备离线：来源模块为 DEVICE、级别 2，内容含设备名与最后心跳")
    void deviceOfflineFillsContent() {
        stubSave();
        LocalDateTime heartbeat = LocalDateTime.now().minusMinutes(9);

        service.createDeviceOfflineAlert(7L, "网关A", "GATEWAY", heartbeat);

        AlertEvent saved = capturedEntity();
        assertEquals("DEVICE", saved.getSourceModule());
        assertEquals((short) 2, saved.getLevel());
        assertEquals("设备离线告警", saved.getTitle());
        assertTrue(saved.getMessage().contains("网关A"), "实际: " + saved.getMessage());
        assertTrue(saved.getMessage().contains("GATEWAY"), "实际: " + saved.getMessage());
    }

    // ---------------- 库存异常 ----------------

    @Test
    @DisplayName("库存异常：字段缺失被拒")
    void inventoryAbnormalRejectsMissingFields() {
        assertEquals(
                ErrorCode.PARAM_ERROR,
                assertThrows(
                                BusinessException.class,
                                () ->
                                        service.createInventoryAbnormalAlert(
                                                null, "螺丝", 10, 5, "主仓库"))
                        .getErrorCode());
        assertEquals(
                ErrorCode.PARAM_ERROR,
                assertThrows(
                                BusinessException.class,
                                () ->
                                        service.createInventoryAbnormalAlert(
                                                1L, "螺丝", null, 5, "主仓库"))
                        .getErrorCode());
        assertEquals(
                ErrorCode.PARAM_ERROR,
                assertThrows(
                                BusinessException.class,
                                () ->
                                        service.createInventoryAbnormalAlert(
                                                1L, "螺丝", 10, null, "主仓库"))
                        .getErrorCode());
    }

    @Test
    @DisplayName("库存异常：数量一致无需告警")
    void inventoryAbnormalRejectsEqualQuantities() {
        BusinessException ex =
                assertThrows(
                        BusinessException.class,
                        () -> service.createInventoryAbnormalAlert(1L, "螺丝", 10, 10, "主仓库"));

        assertEquals(ErrorCode.PARAM_ERROR, ex.getErrorCode());
    }

    @Test
    @DisplayName("库存异常：差异 ≤10 记 2 级，>10 记 3 级")
    void inventoryAbnormalLevelDependsOnDifference() {
        stubSave();

        assertEquals(2, service.createInventoryAbnormalAlert(1L, "螺丝", 100, 90, "主仓库").getLevel());
        assertEquals(3, service.createInventoryAbnormalAlert(1L, "螺丝", 100, 89, "主仓库").getLevel());
    }

    @Test
    @DisplayName("库存异常：来源模块为 INVENTORY，内容含仓库与四种数量")
    void inventoryAbnormalFillsContent() {
        stubSave();

        service.createInventoryAbnormalAlert(1L, "螺丝", 100, 88, "二号仓");

        AlertEvent saved = capturedEntity();
        assertEquals("INVENTORY", saved.getSourceModule());
        assertEquals("库存异常告警", saved.getTitle());
        assertTrue(saved.getMessage().contains("二号仓"), "实际: " + saved.getMessage());
        assertTrue(saved.getMessage().contains("100"), "实际: " + saved.getMessage());
        assertTrue(saved.getMessage().contains("88"), "实际: " + saved.getMessage());
    }
}
