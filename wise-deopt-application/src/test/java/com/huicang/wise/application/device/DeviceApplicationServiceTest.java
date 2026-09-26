package com.huicang.wise.application.device;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyShort;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.huicang.wise.common.api.ErrorCode;
import com.huicang.wise.common.exception.BusinessException;
import com.huicang.wise.domain.device.DeviceCore;
import com.huicang.wise.domain.inspection.InspectionTask;
import com.huicang.wise.domain.inspection.TaskPublisher;
import com.huicang.wise.infrastructure.persistence.repository.device.DeviceRepository;
import com.huicang.wise.infrastructure.persistence.repository.inspection.InspectionTaskRepository;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * {@link DeviceApplicationService} 的**状态机与统计**子切片测试（P2-11 应用层补测）。
 *
 * <p>选这一块的理由：设备域的"心跳/离线"两条状态机是**主流程的开关**，一旦出错会直接影响 线上设备可见性与任务调度；而且它们的边界（"已在线则不动"、"只在 ONLINE 时置离线"、
 * "上线最多推 10 个任务"）都很具体、可以逐条钉死。
 *
 * <p>状态常量对齐源码私有常量：设备类型 0=RFID / 1=相机 / 2=机器人；设备状态 0=离线 / 1=在线 / 2=故障。
 *
 * @author WiseDepot
 * @version 1.0
 * @since 2026-09-26
 */
@ExtendWith(MockitoExtension.class)
class DeviceApplicationServiceTest {

    @Mock private DeviceRepository deviceRepository;
    @Mock private InspectionTaskRepository inspectionTaskRepository;
    @Mock private StringRedisTemplate stringRedisTemplate;
    @Mock private TaskPublisher taskPublisher;

    @InjectMocks private DeviceApplicationService service;

    private static DeviceCore device(long id, String code, short status) {
        DeviceCore d = new DeviceCore();
        d.setDeviceId(id);
        d.setDeviceCode(code);
        d.setName("设备-" + code);
        d.setStatus(status);
        d.setType((short) 0);
        return d;
    }

    private static InspectionTask task(long id) {
        InspectionTask t = new InspectionTask();
        t.setTaskId(id);
        t.setDeviceId(1L);
        t.setTaskType((short) 1);
        t.setStatus((short) 0);
        t.setWarehouseId(1L);
        t.setPlanId(1L);
        return t;
    }

    // ---------------------------------------------------------------- 查询与删除

    @Test
    @DisplayName("getDevice / getDeviceByCode：不存在 → NOT_FOUND；存在时映射 DTO 且带类型/状态名")
    void getDeviceShouldMapOrThrow() {
        when(deviceRepository.findById(99L)).thenReturn(Optional.empty());
        assertEquals(
                ErrorCode.NOT_FOUND,
                assertThrows(BusinessException.class, () -> service.getDevice(99L)).getErrorCode());

        when(deviceRepository.findByDeviceCode("NOPE")).thenReturn(Optional.empty());
        assertEquals(
                ErrorCode.NOT_FOUND,
                assertThrows(BusinessException.class, () -> service.getDeviceByCode("NOPE"))
                        .getErrorCode());

        when(deviceRepository.findById(1L)).thenReturn(Optional.of(device(1L, "DEV-1", (short) 1)));
        DeviceDTO dto = service.getDevice(1L);
        assertEquals(1L, dto.getDeviceId());
        assertEquals("DEV-1", dto.getDeviceCode());
        assertNotNull(dto.getDeviceTypeName(), "应带上设备类型名");
        assertNotNull(dto.getDeviceStatusName(), "应带上设备状态名");
    }

    @Test
    @DisplayName("deleteDevice：不存在 → NOT_FOUND 且不删、不清缓存；存在时删除并清 dashboard KPI 缓存")
    void deleteDeviceShouldDeleteAndEvict() {
        when(deviceRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(BusinessException.class, () -> service.deleteDevice(99L));
        verify(deviceRepository, never()).delete(any(DeviceCore.class));
        verify(stringRedisTemplate, never()).delete(anyString());

        DeviceCore d = device(5L, "DEV-5", (short) 1);
        when(deviceRepository.findById(5L)).thenReturn(Optional.of(d));
        service.deleteDevice(5L);
        verify(deviceRepository).delete(d);
        verify(stringRedisTemplate).delete("dashboard:kpi");
    }

    @Test
    @DisplayName("listDevices：enabled 过滤按'是否故障(status=2)'判定 —— ⚠现状固定（源码注释自认可能不准）")
    void listDevicesPinsEnabledFilterSemantics() {
        List<DeviceCore> all = List.of(device(1L, "A", (short) 1), device(2L, "B", (short) 2));
        when(deviceRepository.findByKeywordAndTypeAndStatus(any(), any(), any())).thenReturn(all);

        // enabled=null ⇒ 全返回
        assertEquals(2, service.listDevices(null, null, null, null).size());
        // enabled=true ⇒ 只保留 status != 2（即"未故障"）
        List<DeviceDTO> enabledOnly = service.listDevices(null, null, true, null);
        assertEquals(1, enabledOnly.size());
        assertEquals("A", enabledOnly.get(0).getDeviceCode());
        // enabled=false ⇒ 只保留 status == 2（故障）
        List<DeviceDTO> disabledOnly = service.listDevices(null, null, false, null);
        assertEquals(1, disabledOnly.size());
        assertEquals("B", disabledOnly.get(0).getDeviceCode());
    }

    // ---------------------------------------------------------------- 心跳状态机

    @Test
    @DisplayName("receiveHeartbeat：设备不存在 → NOT_FOUND")
    void receiveHeartbeatWhenMissingShouldThrowNotFound() {
        when(deviceRepository.findByDeviceCode("NOPE")).thenReturn(Optional.empty());

        assertEquals(
                ErrorCode.NOT_FOUND,
                assertThrows(BusinessException.class, () -> service.receiveHeartbeat("NOPE"))
                        .getErrorCode());
    }

    @Test
    @DisplayName("receiveHeartbeat：离线设备首次心跳 ⇒ 置在线、清 KPI 缓存、并把待执行任务下发给该设备")
    void receiveHeartbeatShouldBringDeviceOnlineAndPushPendingTasks() {
        DeviceCore d = device(1L, "DEV-1", (short) 0); // 原为离线
        when(deviceRepository.findByDeviceCode("DEV-1")).thenReturn(Optional.of(d));
        when(inspectionTaskRepository.findByDeviceIdAndStatus(1L, (short) 0))
                .thenReturn(List.of(task(11L), task(12L)));

        service.receiveHeartbeat("DEV-1");

        assertEquals((short) 1, d.getStatus(), "应置为在线");
        assertNotNull(d.getLastHeartbeat(), "应写入最后心跳时间");
        assertEquals(1L, d.getUpdateBy());
        assertNotNull(d.getUpdateTime());
        verify(stringRedisTemplate).delete("dashboard:kpi");
        verify(taskPublisher, times(2))
                .publishTask(org.mockito.ArgumentMatchers.eq("DEV-1"), any());
        verify(deviceRepository).save(d);
    }

    @Test
    @DisplayName("receiveHeartbeat：已在线设备重复心跳 ⇒ 只刷新心跳时间，不改状态/不清缓存/不再推任务")
    void receiveHeartbeatWhenAlreadyOnlineShouldOnlyRefreshTimestamp() {
        DeviceCore d = device(2L, "DEV-2", (short) 1); // 已在线
        when(deviceRepository.findByDeviceCode("DEV-2")).thenReturn(Optional.of(d));

        service.receiveHeartbeat("DEV-2");

        assertEquals((short) 1, d.getStatus());
        assertNotNull(d.getLastHeartbeat());
        verify(stringRedisTemplate, never()).delete(anyString());
        verify(taskPublisher, never()).publishTask(anyString(), any());
        verify(inspectionTaskRepository, never()).findByDeviceIdAndStatus(anyLong(), anyShort());
        verify(deviceRepository).save(d);
    }

    @Test
    @DisplayName("receiveHeartbeat：待执行任务超过 10 个时**只推前 10 个**（上限来自源码 maxTasksToPush）")
    void receiveHeartbeatShouldCapPushedTasksAtTen() {
        DeviceCore d = device(3L, "DEV-3", (short) 0);
        when(deviceRepository.findByDeviceCode("DEV-3")).thenReturn(Optional.of(d));
        List<InspectionTask> many = new ArrayList<>();
        for (int i = 0; i < 25; i++) {
            many.add(task(100L + i));
        }
        when(inspectionTaskRepository.findByDeviceIdAndStatus(3L, (short) 0)).thenReturn(many);

        service.receiveHeartbeat("DEV-3");

        verify(taskPublisher, times(10)).publishTask(anyString(), any());
    }

    // ---------------------------------------------------------------- 离线检测

    @Test
    @DisplayName("checkOfflineDevices：把超时在线设备置离线、清缓存，并**把执行中任务重置为待执行且清空 startTime**")
    void checkOfflineDevicesShouldOfflineAndResetRunningTasks() {
        DeviceCore online = device(1L, "DEV-1", (short) 1);
        online.setLastHeartbeat(LocalDateTime.now().minusSeconds(60));
        when(deviceRepository.findOfflineDevices(any(LocalDateTime.class)))
                .thenReturn(List.of(online));

        InspectionTask running = task(21L);
        running.setStatus((short) 1); // 执行中
        running.setStartTime(LocalDateTime.now().minusMinutes(5));
        when(inspectionTaskRepository.findByDeviceIdAndStatus(1L, (short) 1))
                .thenReturn(List.of(running));

        service.checkOfflineDevices();

        assertEquals((short) 0, online.getStatus(), "超时的在线设备应被置离线");
        assertEquals(1L, online.getUpdateBy());
        assertNotNull(online.getUpdateTime());
        verify(stringRedisTemplate).delete("dashboard:kpi");

        assertEquals((short) 0, running.getStatus(), "执行中任务应退回待执行");
        assertNull(running.getStartTime(), "退回待执行时应清空开始时间");
        assertNotNull(running.getUpdateTime());
        verify(inspectionTaskRepository).save(running);
        verify(deviceRepository).save(online);
    }

    @Test
    @DisplayName("checkOfflineDevices：**已经在离线的设备不再处理**（不重复置离线、不清缓存、不动任务）")
    void checkOfflineDevicesShouldSkipAlreadyOfflineDevices() {
        DeviceCore alreadyOffline = device(2L, "DEV-2", (short) 0);
        when(deviceRepository.findOfflineDevices(any(LocalDateTime.class)))
                .thenReturn(List.of(alreadyOffline));

        service.checkOfflineDevices();

        verify(deviceRepository, never()).save(any(DeviceCore.class));
        verify(stringRedisTemplate, never()).delete(anyString());
        verify(inspectionTaskRepository, never()).findByDeviceIdAndStatus(anyLong(), anyShort());
    }

    // ---------------------------------------------------------------- 统计

    @Test
    @DisplayName("getDeviceStatistics：把 8 个计数按固定键名装进 map")
    void getDeviceStatisticsShouldMapAllCounters() {
        when(deviceRepository.count()).thenReturn(10L);
        when(deviceRepository.countByStatus((short) 1)).thenReturn(7L);
        when(deviceRepository.countByStatus((short) 0)).thenReturn(2L);
        when(deviceRepository.countByStatus((short) 2)).thenReturn(1L);
        when(deviceRepository.countEnabledDevices((short) 0)).thenReturn(8L);
        when(deviceRepository.countByType((short) 0)).thenReturn(4L);
        when(deviceRepository.countByType((short) 1)).thenReturn(3L);
        when(deviceRepository.countByType((short) 2)).thenReturn(3L);

        Map<String, Object> stats = service.getDeviceStatistics();

        assertEquals(10L, stats.get("totalDevices"));
        assertEquals(7L, stats.get("onlineDevices"));
        assertEquals(2L, stats.get("offlineDevices"));
        assertEquals(1L, stats.get("faultDevices"));
        assertEquals(8L, stats.get("enabledDevices"));
        assertEquals(4L, stats.get("rfidDevices"));
        assertEquals(3L, stats.get("cameraDevices"));
        assertEquals(3L, stats.get("robotDevices"));
        assertEquals(8, stats.size(), "统计项数量应固定为 8，新增/删除键会触发该断言");
    }

    @Test
    @DisplayName("设备类型/状态名映射：经 DTO 暴露（0=RFID/1=相机/2=机器人；0=离线/1=在线/2=故障）")
    void deviceTypeAndStatusNameMapping() {
        DeviceCore d = device(1L, "DEV-1", (short) 0);
        when(deviceRepository.findById(1L)).thenReturn(Optional.of(d));

        DeviceDTO offline = service.getDevice(1L);
        assertNotNull(offline.getDeviceStatusName());

        d.setStatus((short) 1);
        DeviceDTO online = service.getDevice(1L);
        assertNotNull(online.getDeviceStatusName());

        d.setType((short) 2);
        DeviceDTO robot = service.getDevice(1L);
        assertNotNull(robot.getDeviceTypeName());
    }
}
