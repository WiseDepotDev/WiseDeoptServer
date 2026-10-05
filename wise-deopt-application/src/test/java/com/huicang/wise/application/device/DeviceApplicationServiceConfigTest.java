package com.huicang.wise.application.device;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.huicang.wise.application.dashboard.DashboardKpiCache;
import com.huicang.wise.common.api.ErrorCode;
import com.huicang.wise.common.exception.BusinessException;
import com.huicang.wise.domain.device.DeviceConfigPublisher;
import com.huicang.wise.domain.device.DeviceCore;
import com.huicang.wise.domain.device.DeviceInspectionRobot;
import com.huicang.wise.domain.service.DeviceLogStorage;
import com.huicang.wise.infrastructure.persistence.repository.device.DeviceRepository;
import com.huicang.wise.infrastructure.persistence.repository.device.RobotConfigRepository;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * {@link DeviceApplicationService} 的**更新 / 配置下发 / 日志上传**子切片（P2-11 应用层补测，设备域收尾）。
 *
 * <p>要点：
 *
 * <ul>
 *   <li>{@code updateDevice} 是**补丁语义**：只有非 null 字段被覆盖（传 null = 不改），换 IP 会先处理冲突；
 *   <li>{@code getConfig} 的基础配置 + **仅机器人**才追加运动参数与默认值；
 *   <li>{@code updateRobotConfig} 只应用 {@code params} 里**出现过的键**；机器人记录不存在时会**新建一条**； 非法浮点会被 {@code
 *       toFloat} 静默置 null（本测试钉住该现状）；
 *   <li>{@code updateRobotConfig} / {@code uploadLogs} 对下游异常**一律吞掉**（不使事务失败）——
 *       这是源码的明确意图，本测试保证它不被"顺手改成抛异常"。
 * </ul>
 *
 * @author WiseDepot
 * @version 1.0
 * @since 2026-09-27
 */
@ExtendWith(MockitoExtension.class)
class DeviceApplicationServiceConfigTest {

    @Mock private DeviceRepository deviceRepository;
    @Mock private RobotConfigRepository robotRepository;
    @Mock private DashboardKpiCache dashboardKpiCache;
    @Mock private DeviceConfigPublisher deviceConfigPublisher;
    @Mock private DeviceLogStorage deviceLogStorage;

    @InjectMocks private DeviceApplicationService service;

    private static DeviceCore device(long id, String code, short type) {
        DeviceCore d = new DeviceCore();
        d.setDeviceId(id);
        d.setDeviceCode(code);
        d.setName("名称-" + code);
        d.setType(type);
        d.setStatus((short) 1);
        return d;
    }

    // ---------------------------------------------------------------- updateDevice

    @Test
    @DisplayName("updateDevice：设备不存在 → NOT_FOUND")
    void updateDeviceWhenMissingShouldThrowNotFound() {
        DeviceUpdateRequest request = new DeviceUpdateRequest();
        when(deviceRepository.findById(99L)).thenReturn(Optional.empty());

        assertEquals(
                ErrorCode.NOT_FOUND,
                assertThrows(BusinessException.class, () -> service.updateDevice(99L, request))
                        .getErrorCode());
    }

    @Test
    @DisplayName("updateDevice：**补丁语义** —— 只覆盖非 null 字段，传 null 表示不改")
    void updateDeviceShouldApplyOnlyNonNullFields() {
        DeviceCore d = device(1L, "DEV-1", (short) 0);
        d.setRemark("原备注");
        when(deviceRepository.findById(1L)).thenReturn(Optional.of(d));
        when(deviceRepository.save(any(DeviceCore.class))).thenAnswer(inv -> inv.getArgument(0));

        DeviceUpdateRequest request = new DeviceUpdateRequest();
        request.setDeviceStatus((short) 2); // 只改状态
        service.updateDevice(1L, request);

        assertEquals((short) 2, d.getStatus());
        assertEquals("名称-DEV-1", d.getName(), "deviceName 为 null ⇒ 名称不变");
        assertNull(d.getIpAddress(), "ipAddress 为 null ⇒ 不触发冲突处理也不改 IP");
        assertEquals("原备注", d.getRemark(), "remark 为 null ⇒ 备注不变");
        assertEquals(1L, d.getUpdateBy());
        assertNotNull(d.getUpdateTime());
        verify(dashboardKpiCache).invalidate();
    }

    @Test
    @DisplayName("updateDevice：换 IP 时会先处理冲突（占用同一 IP 的**其它**设备被清空 IP）")
    void updateDeviceShouldResolveIpConflictBeforeAssigning() {
        DeviceCore target = device(1L, "DEV-1", (short) 1);
        DeviceCore other = device(2L, "DEV-2", (short) 1);
        other.setIpAddress("10.0.0.9");
        when(deviceRepository.findById(1L)).thenReturn(Optional.of(target));
        when(deviceRepository.findByIpAddress("10.0.0.9")).thenReturn(Optional.of(other));
        when(deviceRepository.save(any(DeviceCore.class))).thenAnswer(inv -> inv.getArgument(0));

        DeviceUpdateRequest request = new DeviceUpdateRequest();
        request.setIpAddress("10.0.0.9");
        service.updateDevice(1L, request);

        assertNull(other.getIpAddress(), "冲突设备的 IP 应被清空");
        verify(deviceRepository).saveAndFlush(other);
        assertEquals("10.0.0.9", target.getIpAddress());
    }

    // ---------------------------------------------------------------- getConfig

    @Test
    @DisplayName("getConfig：基础配置固定 6 项；**非机器人**不追加运动参数")
    void getConfigShouldReturnBaseConfigForNonRobot() {
        when(deviceRepository.findByDeviceCode("R1"))
                .thenReturn(Optional.of(device(1L, "R1", (short) 0)));

        Map<String, Object> cfg = service.getConfig("R1", "latest");

        assertEquals("1.0.1", cfg.get("version"));
        assertEquals(1, cfg.get("heartbeatInterval"));
        assertEquals("/api/device", cfg.get("apiBaseUrl"));
        assertEquals(3, cfg.get("taskPollInterval"));
        assertEquals("periodic", cfg.get("logUploadStrategy"));
        assertEquals(2000, cfg.get("networkTimeout"));
        assertEquals(6, cfg.size(), "非机器人应只有基础 6 项");
    }

    @Test
    @DisplayName("getConfig：设备不存在时也返回基础配置（不抛异常）")
    void getConfigShouldTolerateUnknownDevice() {
        when(deviceRepository.findByDeviceCode("NOPE")).thenReturn(Optional.empty());

        Map<String, Object> cfg = service.getConfig("NOPE", "latest");

        assertEquals(6, cfg.size());
    }

    @Test
    @DisplayName("getConfig：机器人设备追加运动参数；未配置的字段用默认值补齐")
    void getConfigShouldMergeRobotConfigAndDefaults() {
        when(deviceRepository.findByDeviceCode("ROBOT"))
                .thenReturn(Optional.of(device(7L, "ROBOT", (short) 2)));
        DeviceInspectionRobot robot = new DeviceInspectionRobot();
        robot.setDeviceId(7L);
        robot.setMoveSpeedCmS(25.5f); // 只给了速度，其余为 null
        when(robotRepository.findById(7L)).thenReturn(Optional.of(robot));

        Map<String, Object> cfg = service.getConfig("ROBOT", "latest");

        assertEquals(25.5f, cfg.get("move_speed_cm_s"), "已配置值应覆盖默认值");
        assertEquals(0.3f, cfg.get("motor_trim_a"), "未配置的字段应补默认值");
        assertEquals(-0.195f, cfg.get("motor_trim_b"));
        assertEquals(11, cfg.size(), "基础 6 项 + 运动参数 5 项");
    }

    // ---------------------------------------------------------------- updateRobotConfig

    @Test
    @DisplayName("updateRobotConfig：设备不存在 → NOT_FOUND；非机器人 → PARAM_ERROR")
    void updateRobotConfigShouldRejectMissingOrNonRobot() {
        when(deviceRepository.findByDeviceCode("NOPE")).thenReturn(Optional.empty());
        assertEquals(
                ErrorCode.NOT_FOUND,
                assertThrows(
                                BusinessException.class,
                                () -> service.updateRobotConfig("NOPE", new HashMap<>()))
                        .getErrorCode());

        when(deviceRepository.findByDeviceCode("CAM"))
                .thenReturn(Optional.of(device(1L, "CAM", (short) 1)));
        assertEquals(
                ErrorCode.PARAM_ERROR,
                assertThrows(
                                BusinessException.class,
                                () -> service.updateRobotConfig("CAM", new HashMap<>()))
                        .getErrorCode());
    }

    @Test
    @DisplayName("updateRobotConfig：**只应用 params 里出现过的键**；Number 与数字字符串都能解析")
    void updateRobotConfigShouldApplyOnlyProvidedKeys() {
        when(deviceRepository.findByDeviceCode("ROBOT"))
                .thenReturn(Optional.of(device(7L, "ROBOT", (short) 2)));
        DeviceInspectionRobot robot = new DeviceInspectionRobot();
        robot.setDeviceId(7L);
        robot.setMotorTrimC(9.9f); // 未被本请求覆盖，应保持
        when(robotRepository.findById(7L)).thenReturn(Optional.of(robot));

        Map<String, Object> params = new HashMap<>();
        params.put("move_speed_cm_s", 20); // Number
        params.put("motor_trim_a", "0.25"); // 数字字符串
        service.updateRobotConfig("ROBOT", params);

        assertEquals(20f, robot.getMoveSpeedCmS());
        assertEquals(0.25f, robot.getMotorTrimA());
        assertEquals(9.9f, robot.getMotorTrimC(), "未出现在 params 里的字段不应被改动");
        assertNull(robot.getMotorTrimB(), "未提供的字段保持原值（此处原为 null）");
        assertEquals(1L, robot.getUpdateBy());
        assertNotNull(robot.getUpdateTime());
        verify(robotRepository).save(robot);
    }

    @Test
    @DisplayName("updateRobotConfig：⚠现状固定 —— **非法浮点会被静默置 null**（不报错、不保留原值）")
    void updateRobotConfigShouldSilentlyNullInvalidFloat() {
        when(deviceRepository.findByDeviceCode("ROBOT"))
                .thenReturn(Optional.of(device(7L, "ROBOT", (short) 2)));
        DeviceInspectionRobot robot = new DeviceInspectionRobot();
        robot.setDeviceId(7L);
        robot.setMoveSpeedCmS(19.7f);
        when(robotRepository.findById(7L)).thenReturn(Optional.of(robot));

        Map<String, Object> params = new HashMap<>();
        params.put("move_speed_cm_s", "not-a-number");
        service.updateRobotConfig("ROBOT", params);

        assertNull(robot.getMoveSpeedCmS(), "非法值被 toFloat 转成 null 并直接写入 —— 属现状（未改）");
    }

    @Test
    @DisplayName("updateRobotConfig：机器人记录不存在时**新建一条**（带 deviceId 与 createBy）")
    void updateRobotConfigShouldCreateRobotRecordWhenAbsent() {
        when(deviceRepository.findByDeviceCode("ROBOT"))
                .thenReturn(Optional.of(device(8L, "ROBOT", (short) 2)));
        when(robotRepository.findById(8L)).thenReturn(Optional.empty());

        Map<String, Object> params = new HashMap<>();
        params.put("move_speed_cm_s", 19.7);
        service.updateRobotConfig("ROBOT", params);

        ArgumentCaptor<DeviceInspectionRobot> captor =
                ArgumentCaptor.forClass(DeviceInspectionRobot.class);
        verify(robotRepository).save(captor.capture());
        DeviceInspectionRobot created = captor.getValue();
        assertEquals(8L, created.getDeviceId());
        assertEquals(1L, created.getCreateBy());
        assertNotNull(created.getCreateTime());
        assertEquals(19.7f, created.getMoveSpeedCmS());
    }

    // ---------------------------------------------------------------- 下游异常一律吞掉

    @Test
    @DisplayName("updateRobotConfig：**推送失败不影响本次配置保存**（异常被吞，不抛出）")
    void updateRobotConfigShouldSwallowPushFailure() {
        when(deviceRepository.findByDeviceCode("ROBOT"))
                .thenReturn(Optional.of(device(7L, "ROBOT", (short) 2)));
        DeviceInspectionRobot robot = new DeviceInspectionRobot();
        robot.setDeviceId(7L);
        when(robotRepository.findById(7L)).thenReturn(Optional.of(robot));
        doThrow(new RuntimeException("MQTT down"))
                .when(deviceConfigPublisher)
                .publishConfig(anyString(), any());

        assertDoesNotThrow(() -> service.updateRobotConfig("ROBOT", new HashMap<>()));
        verify(robotRepository).save(robot);
    }

    @Test
    @DisplayName("pushConfigUpdate：发布者未注入时**静默跳过**（不抛 NPE）")
    void pushConfigUpdateShouldNoOpWithoutPublisher() {
        ReflectionTestUtils.setField(service, "deviceConfigPublisher", null);

        assertDoesNotThrow(() -> service.pushConfigUpdate("DEV-1"));
    }

    @Test
    @DisplayName("uploadLogs：**存储缺失或下游抛异常时都不报错**（本方法把失败一律吞掉）")
    void uploadLogsShouldNeverPropagateFailures() {
        // 分支一：存储服务未注入 ⇒ 只记 warn，直接返回
        ReflectionTestUtils.setField(service, "deviceLogStorage", null);
        assertDoesNotThrow(() -> service.uploadLogs("DEV-1", "日志"));

        // 分支二：存储抛异常 ⇒ 被 catch 吞掉，调用方不受影响
        ReflectionTestUtils.setField(service, "deviceLogStorage", deviceLogStorage);
        doThrow(new RuntimeException("MinIO down"))
                .when(deviceLogStorage)
                .storeLog(anyString(), anyString(), any(), any(Integer.class));
        assertDoesNotThrow(() -> service.uploadLogs("DEV-2", "x"));
    }
}
