package com.huicang.wise.application.device;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyShort;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.huicang.wise.application.dashboard.DashboardKpiCache;
import com.huicang.wise.common.api.ErrorCode;
import com.huicang.wise.common.exception.BusinessException;
import com.huicang.wise.domain.device.DeviceCore;
import com.huicang.wise.domain.inspection.InspectionTask;
import com.huicang.wise.domain.inspection.TaskPublisher;
import com.huicang.wise.infrastructure.persistence.repository.device.DeviceRepository;
import com.huicang.wise.infrastructure.persistence.repository.inspection.InspectionTaskRepository;
import com.huicang.wise.infrastructure.security.JwtTokenProvider;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * {@link DeviceApplicationService#createDevice} 单元测试（P2-11 应用层补测）—— 设备域的**注册 / 重复注册**入口，含 IP
 * 冲突处理与任务补发。
 *
 * <p>该方法约 118 行、两条主分支（**新建设备** 与 **重复注册**），且都要签发 access/refresh token； 边界集中在：参数裁剪、IP 冲突时清旧设备的
 * IP、"推送待执行任务的上限 10"。
 *
 * @author WiseDepot
 * @version 1.0
 * @since 2026-09-26
 */
@ExtendWith(MockitoExtension.class)
class DeviceApplicationServiceCreateTest {

    @Mock private DeviceRepository deviceRepository;
    @Mock private InspectionTaskRepository inspectionTaskRepository;
    @Mock private DashboardKpiCache dashboardKpiCache;
    @Mock private TaskPublisher taskPublisher;
    @Mock private JwtTokenProvider jwtTokenProvider;

    @InjectMocks private DeviceApplicationService service;

    private void stubTokens() {
        lenient()
                .when(jwtTokenProvider.generateAccessToken(anyString(), anyLong()))
                .thenReturn("ACCESS");
        lenient()
                .when(jwtTokenProvider.generateRefreshToken(anyString(), anyLong()))
                .thenReturn("REFRESH");
    }

    private static DeviceCreateRequest request(String code, String ip) {
        DeviceCreateRequest r = new DeviceCreateRequest();
        r.setDeviceCode(code);
        r.setDeviceName("名称-" + code);
        r.setDeviceType((short) 0);
        r.setIpAddress(ip);
        r.setRemark("备注");
        return r;
    }

    private static InspectionTask task(long id) {
        InspectionTask t = new InspectionTask();
        t.setTaskId(id);
        t.setDeviceId(1L);
        t.setTaskType((short) 1);
        t.setWarehouseId(1L);
        t.setPlanId(1L);
        return t;
    }

    // ---------------------------------------------------------------- 参数校验与 IP 冲突

    @Test
    @DisplayName("createDevice：request 为 null → PARAM_ERROR")
    void createDeviceWithNullRequestShouldThrowParamError() {
        BusinessException ex =
                assertThrows(BusinessException.class, () -> service.createDevice(null));

        assertEquals(ErrorCode.PARAM_ERROR, ex.getErrorCode());
    }

    @Test
    @DisplayName("IP 冲突：该 IP 属于**另一个**设备 ⇒ 清空旧设备的 IP 并立即落库")
    void ipConflictShouldClearIpFromOtherDevice() {
        stubTokens();
        DeviceCore other = new DeviceCore();
        other.setDeviceId(9L);
        other.setDeviceCode("OTHER");
        other.setIpAddress("10.0.0.5");
        when(deviceRepository.findByIpAddress("10.0.0.5")).thenReturn(Optional.of(other));
        when(deviceRepository.existsByDeviceCode("NEW")).thenReturn(false);
        when(deviceRepository.save(any(DeviceCore.class))).thenAnswer(inv -> inv.getArgument(0));

        service.createDevice(request("NEW", "  10.0.0.5  ")); // 检查顺带验证 trim

        assertNull(other.getIpAddress(), "冲突设备的 IP 应被清空");
        assertEquals(1L, other.getUpdateBy());
        assertNotNull(other.getUpdateTime());
        verify(deviceRepository).saveAndFlush(other);
    }

    @Test
    @DisplayName("IP 冲突：该 IP 就属于**自己** ⇒ 不动任何设备（不 saveAndFlush）")
    void ipConflictWithSelfShouldDoNothing() {
        stubTokens();
        DeviceCore self = new DeviceCore();
        self.setDeviceId(1L);
        self.setDeviceCode("SELF");
        self.setIpAddress("10.0.0.6");
        when(deviceRepository.findByIpAddress("10.0.0.6")).thenReturn(Optional.of(self));
        when(deviceRepository.existsByDeviceCode("SELF")).thenReturn(false);
        when(deviceRepository.save(any(DeviceCore.class))).thenAnswer(inv -> inv.getArgument(0));

        service.createDevice(request("SELF", "10.0.0.6"));

        verify(deviceRepository, never()).saveAndFlush(any(DeviceCore.class));
    }

    @Test
    @DisplayName("IP 为空或 null：跳过冲突检查（不查 findByIpAddress）")
    void blankIpShouldSkipConflictCheck() {
        stubTokens();
        when(deviceRepository.existsByDeviceCode("NOIP")).thenReturn(false);
        when(deviceRepository.save(any(DeviceCore.class))).thenAnswer(inv -> inv.getArgument(0));

        service.createDevice(request("NOIP", "   "));

        verify(deviceRepository, never()).findByIpAddress(anyString());
    }

    // ---------------------------------------------------------------- 新建设备

    @Test
    @DisplayName("新建设备：status=ONLINE、createBy/updateBy=1、写心跳与时间戳，清 KPI 缓存并签发双 token")
    void createDeviceShouldPersistOnlineDeviceWithTokens() {
        stubTokens();
        when(deviceRepository.findByIpAddress("10.0.0.7")).thenReturn(Optional.empty());
        when(deviceRepository.existsByDeviceCode("NEW-1")).thenReturn(false);
        // 模拟数据库在 save 时分配主键：签发 token 需要 deviceId，而新建对象初始为 null
        // （注意 `anyLong()` 不匹配 null，所以不分配主键时 token 桩会落空 ⇒ 断言会看到 null）
        when(deviceRepository.save(any(DeviceCore.class)))
                .thenAnswer(
                        inv -> {
                            DeviceCore d = inv.getArgument(0);
                            if (d.getDeviceId() == null) {
                                d.setDeviceId(100L);
                            }
                            return d;
                        });

        DeviceDTO dto = service.createDevice(request("NEW-1", "10.0.0.7"));

        ArgumentCaptor<DeviceCore> captor = ArgumentCaptor.forClass(DeviceCore.class);
        verify(deviceRepository).save(captor.capture());
        DeviceCore saved = captor.getValue();
        assertEquals("NEW-1", saved.getDeviceCode());
        assertEquals("10.0.0.7", saved.getIpAddress(), "IP 应已 trim");
        assertEquals((short) 1, saved.getStatus(), "新注册设备视为在线");
        assertEquals(1L, saved.getCreateBy());
        assertEquals(1L, saved.getUpdateBy());
        assertNotNull(saved.getCreateTime());
        assertNotNull(saved.getLastHeartbeat());

        verify(dashboardKpiCache).invalidate();
        assertEquals("ACCESS", dto.getToken(), "应签发 access token");
        assertEquals("REFRESH", dto.getRefreshToken(), "应签发 refresh token");
    }

    // ---------------------------------------------------------------- 重复注册

    @Test
    @DisplayName("重复注册：复用既有设备（不新建），刷新名称/IP/备注与心跳、置在线，并补发待执行任务")
    void reRegisterShouldReuseExistingDeviceAndPushTasks() {
        stubTokens();
        DeviceCore existing = new DeviceCore();
        existing.setDeviceId(5L);
        existing.setDeviceCode("RE-1");
        existing.setStatus((short) 0); // 原为离线
        when(deviceRepository.findByIpAddress("10.0.0.8")).thenReturn(Optional.empty());
        when(deviceRepository.existsByDeviceCode("RE-1")).thenReturn(true);
        when(deviceRepository.findByDeviceCode("RE-1")).thenReturn(Optional.of(existing));
        when(deviceRepository.save(any(DeviceCore.class))).thenAnswer(inv -> inv.getArgument(0));
        when(inspectionTaskRepository.findByDeviceIdAndStatus(5L, (short) 0))
                .thenReturn(List.of(task(31L), task(32L)));

        DeviceDTO dto = service.createDevice(request("RE-1", "10.0.0.8"));

        assertEquals((short) 1, existing.getStatus(), "重复注册应把设备置为在线");
        assertEquals("10.0.0.8", existing.getIpAddress());
        assertEquals("名称-RE-1", existing.getName());
        assertEquals(1L, existing.getUpdateBy());
        assertNotNull(existing.getLastHeartbeat());
        verify(dashboardKpiCache).invalidate();
        verify(taskPublisher, times(2)).publishTask(org.mockito.ArgumentMatchers.eq("RE-1"), any());
        assertEquals("ACCESS", dto.getToken());
        assertEquals("REFRESH", dto.getRefreshToken());
    }

    @Test
    @DisplayName("重复注册：待执行任务超过 10 个时**只补发前 10 个**")
    void reRegisterShouldCapPushedTasksAtTen() {
        stubTokens();
        DeviceCore existing = new DeviceCore();
        existing.setDeviceId(6L);
        existing.setDeviceCode("RE-2");
        existing.setStatus((short) 1);
        when(deviceRepository.findByIpAddress("10.0.0.9")).thenReturn(Optional.empty());
        when(deviceRepository.existsByDeviceCode("RE-2")).thenReturn(true);
        when(deviceRepository.findByDeviceCode("RE-2")).thenReturn(Optional.of(existing));
        when(deviceRepository.save(any(DeviceCore.class))).thenAnswer(inv -> inv.getArgument(0));
        List<InspectionTask> many = new ArrayList<>();
        for (int i = 0; i < 15; i++) {
            many.add(task(200L + i));
        }
        when(inspectionTaskRepository.findByDeviceIdAndStatus(6L, (short) 0)).thenReturn(many);

        service.createDevice(request("RE-2", "10.0.0.9"));

        verify(taskPublisher, times(10)).publishTask(anyString(), any());
    }

    @Test
    @DisplayName("重复注册：既有编码存在但按编码查不到 → NOT_FOUND（不静默新建）")
    void reRegisterWhenLookupFailsShouldThrowNotFound() {
        DeviceCreateRequest req = request("RE-3", null);
        when(deviceRepository.existsByDeviceCode("RE-3")).thenReturn(true);
        when(deviceRepository.findByDeviceCode("RE-3")).thenReturn(Optional.empty());

        BusinessException ex =
                assertThrows(BusinessException.class, () -> service.createDevice(req));

        assertEquals(ErrorCode.NOT_FOUND, ex.getErrorCode());
        verify(deviceRepository, never()).save(any(DeviceCore.class));
        verify(inspectionTaskRepository, never()).findByDeviceIdAndStatus(anyLong(), anyShort());
    }
}
