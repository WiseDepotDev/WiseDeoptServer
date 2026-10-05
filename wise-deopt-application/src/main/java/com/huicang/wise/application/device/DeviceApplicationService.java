package com.huicang.wise.application.device;

import com.huicang.wise.application.dashboard.DashboardKpiCache;
import com.huicang.wise.common.api.ErrorCode;
import com.huicang.wise.common.exception.BusinessException;
import com.huicang.wise.domain.device.DeviceConfigPublisher;
import com.huicang.wise.domain.device.DeviceCore;
import com.huicang.wise.domain.device.DeviceInspectionRobot;
import com.huicang.wise.domain.inspection.InspectionTask;
import com.huicang.wise.domain.inspection.TaskMessage;
import com.huicang.wise.domain.inspection.TaskPublisher;
import com.huicang.wise.infrastructure.persistence.repository.device.DeviceRepository;
import com.huicang.wise.infrastructure.persistence.repository.device.RobotConfigRepository;
import com.huicang.wise.infrastructure.persistence.repository.inspection.InspectionTaskRepository;
import com.huicang.wise.infrastructure.redis.annotation.CacheEvict;
import com.huicang.wise.infrastructure.redis.annotation.Cacheable;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 类功能描述：设备应用服务
 *
 * @author xingchentye
 * @date 2026-02-27
 */
@Service
public class DeviceApplicationService {

    private static final Logger logger = LoggerFactory.getLogger(DeviceApplicationService.class);

    @Autowired private DeviceRepository deviceRepository;

    @Autowired private RobotConfigRepository robotRepository;

    @Autowired(required = false)
    private DeviceConfigPublisher deviceConfigPublisher;

    @Autowired private com.huicang.wise.infrastructure.security.JwtTokenProvider jwtTokenProvider;

    @Autowired(required = false)
    private com.huicang.wise.domain.service.DeviceLogStorage deviceLogStorage;

    @Autowired private InspectionTaskRepository inspectionTaskRepository;

    @Autowired private TaskPublisher taskPublisher;

    @Autowired private DashboardKpiCache dashboardKpiCache;

    private static final short DEVICE_TYPE_RFID = 0;
    private static final short DEVICE_TYPE_CAMERA = 1;
    private static final short DEVICE_TYPE_ROBOT = 2;

    private static final short DEVICE_STATUS_OFFLINE = 0;
    private static final short DEVICE_STATUS_ONLINE = 1;
    private static final short DEVICE_STATUS_FAULT = 2;

    private static final int HEARTBEAT_TIMEOUT_SECONDS = 2;

    private void handleIpConflict(String ipAddress, String currentDeviceCode) {
        if (ipAddress == null || ipAddress.isEmpty()) {
            return;
        }
        String trimmedIp = ipAddress.trim();
        logger.info("Checking for IP conflict: {}", trimmedIp);
        deviceRepository
                .findByIpAddress(trimmedIp)
                .ifPresentOrElse(
                        device -> {
                            if (!device.getDeviceCode().equals(currentDeviceCode)) {
                                logger.warn(
                                        "IP conflict: Device {} already uses IP {}. Clearing IP from old device.",
                                        device.getDeviceCode(),
                                        trimmedIp);
                                device.setIpAddress(null);
                                device.setUpdateBy(1L);
                                device.setUpdateTime(LocalDateTime.now());
                                deviceRepository.saveAndFlush(device);
                            } else {
                                logger.info(
                                        "Device {} already owns IP {}",
                                        currentDeviceCode,
                                        trimmedIp);
                            }
                        },
                        () -> logger.info("No existing device found with IP {}", trimmedIp));
    }

    @Transactional
    public DeviceDTO createDevice(DeviceCreateRequest request) throws BusinessException {
        if (request == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "请求参数不能为空");
        }

        if (request.getIpAddress() != null) {
            request.setIpAddress(request.getIpAddress().trim());
        }

        handleIpConflict(request.getIpAddress(), request.getDeviceCode());

        // 如果设备已存在，则更新信息（支持重复注册/重启场景）
        // 实际生产中应有独立的 Login 接口或更严格的认证
        if (deviceRepository.existsByDeviceCode(request.getDeviceCode())) {
            DeviceCore existingDevice =
                    deviceRepository
                            .findByDeviceCode(request.getDeviceCode())
                            .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "设备不存在"));
            existingDevice.setName(request.getDeviceName());
            existingDevice.setIpAddress(request.getIpAddress());
            existingDevice.setRemark(request.getRemark());
            existingDevice.setLastHeartbeat(LocalDateTime.now());
            existingDevice.setStatus(DEVICE_STATUS_ONLINE);
            existingDevice.setUpdateBy(1L);
            existingDevice.setUpdateTime(LocalDateTime.now());
            existingDevice = deviceRepository.save(existingDevice);

            // Clear dashboard KPI cache to ensure data overview updates
            dashboardKpiCache.invalidate();

            logger.info(
                    "设备重新注册/上线 - 设备编码: {}, 设备名称: {}, 原状态: {}, 新状态: {}",
                    existingDevice.getDeviceCode(),
                    existingDevice.getName(),
                    getDeviceStatusName(existingDevice.getStatus()),
                    getDeviceStatusName(DEVICE_STATUS_ONLINE));

            // 设备上线，推送未完成的任务（最多推送10个）
            List<InspectionTask> pendingTasks =
                    inspectionTaskRepository.findByDeviceIdAndStatus(
                            existingDevice.getDeviceId(), (short) 0);
            int maxTasksToPush = 10;
            int pushedCount = 0;
            for (InspectionTask task : pendingTasks) {
                if (pushedCount >= maxTasksToPush) {
                    logger.warn(
                            "设备上线推送任务达到上限 - 设备编码: {}, 已推送: {}, 待推送: {}",
                            existingDevice.getDeviceCode(),
                            pushedCount,
                            pendingTasks.size() - pushedCount);
                    break;
                }
                TaskMessage message =
                        new TaskMessage(
                                task.getTaskId(),
                                task.getTaskType(),
                                task.getTargetDistance(),
                                task.getPlanId(),
                                task.getWarehouseId());
                taskPublisher.publishTask(existingDevice.getDeviceCode(), message);
                logger.info(
                        "设备上线推送任务 - 任务ID: {}, 设备编码: {}",
                        task.getTaskId(),
                        existingDevice.getDeviceCode());
                pushedCount++;
            }

            DeviceDTO dto = toDeviceDTO(existingDevice);
            String accessToken =
                    jwtTokenProvider.generateAccessToken(
                            existingDevice.getDeviceCode(), existingDevice.getDeviceId());
            String refreshToken =
                    jwtTokenProvider.generateRefreshToken(
                            existingDevice.getDeviceCode(), existingDevice.getDeviceId());
            dto.setToken(accessToken);
            dto.setRefreshToken(refreshToken);
            return dto;
        }

        DeviceCore device = new DeviceCore();
        device.setDeviceCode(request.getDeviceCode());
        device.setName(request.getDeviceName());
        device.setType(request.getDeviceType());
        device.setIpAddress(request.getIpAddress());
        device.setStatus(DEVICE_STATUS_ONLINE);
        device.setLastHeartbeat(LocalDateTime.now());
        device.setRemark(request.getRemark());
        device.setCreateBy(1L);
        device.setCreateTime(LocalDateTime.now());
        device.setUpdateBy(1L);
        device.setUpdateTime(LocalDateTime.now());

        DeviceCore savedDevice = deviceRepository.save(device);

        // Clear dashboard KPI cache to ensure data overview updates
        dashboardKpiCache.invalidate();

        logger.info(
                "设备注册 - 设备编码: {}, 设备名称: {}, 设备类型: {}, IP地址: {}",
                savedDevice.getDeviceCode(),
                savedDevice.getName(),
                getDeviceTypeName(savedDevice.getType()),
                savedDevice.getIpAddress());

        DeviceDTO dto = toDeviceDTO(savedDevice);
        String accessToken =
                jwtTokenProvider.generateAccessToken(
                        savedDevice.getDeviceCode(), savedDevice.getDeviceId());
        String refreshToken =
                jwtTokenProvider.generateRefreshToken(
                        savedDevice.getDeviceCode(), savedDevice.getDeviceId());
        dto.setToken(accessToken);
        dto.setRefreshToken(refreshToken);
        return dto;
    }

    @Transactional
    @CacheEvict(prefix = "device", key = "#deviceId", allEntries = false)
    public DeviceDTO updateDevice(Long deviceId, DeviceUpdateRequest request)
            throws BusinessException {
        DeviceCore device =
                deviceRepository
                        .findById(deviceId)
                        .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "设备不存在"));

        if (request.getDeviceName() != null) {
            device.setName(request.getDeviceName());
        }

        if (request.getIpAddress() != null) {
            handleIpConflict(request.getIpAddress(), device.getDeviceCode());
            device.setIpAddress(request.getIpAddress());
        }

        if (request.getDeviceStatus() != null) {
            device.setStatus(request.getDeviceStatus());
        }

        if (request.getRemark() != null) {
            device.setRemark(request.getRemark());
        }

        device.setUpdateBy(1L);
        device.setUpdateTime(LocalDateTime.now());

        DeviceCore savedDevice = deviceRepository.save(device);

        // Clear dashboard KPI cache to ensure data overview updates
        dashboardKpiCache.invalidate();

        return toDeviceDTO(savedDevice);
    }

    @CacheEvict(prefix = "device", key = "#deviceId", allEntries = false)
    @Transactional
    public void deleteDevice(Long deviceId) throws BusinessException {
        DeviceCore device =
                deviceRepository
                        .findById(deviceId)
                        .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "设备不存在"));

        deviceRepository.delete(device);

        // Clear dashboard KPI cache to ensure data overview updates
        dashboardKpiCache.invalidate();
    }

    @Cacheable(prefix = "device", key = "#deviceId", timeout = 1800)
    public DeviceDTO getDevice(Long deviceId) throws BusinessException {
        DeviceCore device =
                deviceRepository
                        .findById(deviceId)
                        .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "设备不存在"));

        return toDeviceDTO(device);
    }

    @Cacheable(prefix = "device:code", key = "#deviceCode", timeout = 1800)
    public DeviceDTO getDeviceByCode(String deviceCode) throws BusinessException {
        DeviceCore device =
                deviceRepository
                        .findByDeviceCode(deviceCode)
                        .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "设备不存在"));

        return toDeviceDTO(device);
    }

    public List<DeviceDTO> listDevices(
            Short deviceType, Short deviceStatus, Boolean enabled, String keyword) {
        List<DeviceCore> devices =
                deviceRepository.findByKeywordAndTypeAndStatus(keyword, deviceType, deviceStatus);

        return devices.stream()
                .filter(
                        d ->
                                enabled == null
                                        || (enabled
                                                ? d.getStatus() != 2
                                                : d.getStatus()
                                                        == 2)) // This filter logic for 'enabled'
                // might be wrong, based on previous
                // code.
                .map(this::toDeviceDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public void receiveHeartbeat(String deviceCode) throws BusinessException {
        DeviceCore device =
                deviceRepository
                        .findByDeviceCode(deviceCode)
                        .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "设备不存在"));

        Short oldStatus = device.getStatus();
        device.setLastHeartbeat(LocalDateTime.now());
        device.setUpdateBy(1L);
        device.setUpdateTime(LocalDateTime.now());

        if (DEVICE_STATUS_ONLINE != oldStatus) {
            device.setStatus(DEVICE_STATUS_ONLINE);

            // Clear dashboard KPI cache to ensure data overview updates
            dashboardKpiCache.invalidate();

            logger.info(
                    "设备上线 - 设备编码: {}, 设备名称: {}, 原状态: {}, 新状态: {}",
                    device.getDeviceCode(),
                    device.getName(),
                    getDeviceStatusName(oldStatus),
                    getDeviceStatusName(DEVICE_STATUS_ONLINE));

            // 设备上线，推送未完成的任务（最多推送10个）
            List<InspectionTask> pendingTasks =
                    inspectionTaskRepository.findByDeviceIdAndStatus(
                            device.getDeviceId(), (short) 0);
            int maxTasksToPush = 10;
            int pushedCount = 0;
            for (InspectionTask task : pendingTasks) {
                if (pushedCount >= maxTasksToPush) {
                    logger.warn(
                            "设备上线推送任务达到上限 - 设备编码: {}, 已推送: {}, 待推送: {}",
                            device.getDeviceCode(),
                            pushedCount,
                            pendingTasks.size() - pushedCount);
                    break;
                }
                TaskMessage message =
                        new TaskMessage(
                                task.getTaskId(),
                                task.getTaskType(),
                                task.getTargetDistance(),
                                task.getPlanId(),
                                task.getWarehouseId());
                taskPublisher.publishTask(device.getDeviceCode(), message);
                logger.info(
                        "设备上线推送任务 - 任务ID: {}, 设备编码: {}", task.getTaskId(), device.getDeviceCode());
                pushedCount++;
            }
        }

        deviceRepository.save(device);
    }

    public Map<String, Object> getDeviceStatistics() {
        Map<String, Object> statistics = new HashMap<>();

        Long totalDevices = deviceRepository.count();
        Long onlineDevices = deviceRepository.countByStatus(DEVICE_STATUS_ONLINE);
        Long offlineDevices = deviceRepository.countByStatus(DEVICE_STATUS_OFFLINE);
        Long faultDevices = deviceRepository.countByStatus(DEVICE_STATUS_FAULT);
        Long enabledDevices = deviceRepository.countEnabledDevices((short) 0);

        Long rfidDevices = deviceRepository.countByType(DEVICE_TYPE_RFID);
        Long cameraDevices = deviceRepository.countByType(DEVICE_TYPE_CAMERA);
        Long robotDevices = deviceRepository.countByType(DEVICE_TYPE_ROBOT);

        statistics.put("totalDevices", totalDevices);
        statistics.put("onlineDevices", onlineDevices);
        statistics.put("offlineDevices", offlineDevices);
        statistics.put("faultDevices", faultDevices);
        statistics.put("enabledDevices", enabledDevices);

        statistics.put("rfidDevices", rfidDevices);
        statistics.put("cameraDevices", cameraDevices);
        statistics.put("robotDevices", robotDevices);

        return statistics;
    }

    @Scheduled(fixedRate = 1000)
    @Transactional
    public void checkOfflineDevices() {
        LocalDateTime thresholdTime = LocalDateTime.now().minusSeconds(HEARTBEAT_TIMEOUT_SECONDS);
        List<DeviceCore> offlineDevices = deviceRepository.findOfflineDevices(thresholdTime);

        for (DeviceCore device : offlineDevices) {
            if (DEVICE_STATUS_ONLINE == device.getStatus()) {
                device.setStatus(DEVICE_STATUS_OFFLINE);
                device.setUpdateBy(1L);
                device.setUpdateTime(LocalDateTime.now());
                deviceRepository.save(device);

                // Clear dashboard KPI cache to ensure data overview updates
                dashboardKpiCache.invalidate();

                logger.info(
                        "设备离线 - 设备编码: {}, 设备名称: {}, 原状态: {}, 新状态: {}, 最后心跳时间: {}",
                        device.getDeviceCode(),
                        device.getName(),
                        getDeviceStatusName(DEVICE_STATUS_ONLINE),
                        getDeviceStatusName(DEVICE_STATUS_OFFLINE),
                        device.getLastHeartbeat());

                // 重置该设备所有"执行中"(status=1)的任务为"待执行"(status=0)
                List<InspectionTask> runningTasks =
                        inspectionTaskRepository.findByDeviceIdAndStatus(
                                device.getDeviceId(), (short) 1);
                for (InspectionTask task : runningTasks) {
                    task.setStatus((short) 0);
                    task.setStartTime(null); // 清除开始时间
                    task.setUpdateTime(LocalDateTime.now());
                    inspectionTaskRepository.save(task);
                    logger.info(
                            "设备离线重置任务 - 任务ID: {}, 设备ID: {}",
                            task.getTaskId(),
                            device.getDeviceId());
                }
            }
        }
    }

    private DeviceDTO toDeviceDTO(DeviceCore device) {
        DeviceDTO dto = new DeviceDTO();
        dto.setDeviceId(device.getDeviceId());
        dto.setDeviceCode(device.getDeviceCode());
        dto.setDeviceName(device.getName());
        dto.setDeviceType(device.getType());
        dto.setDeviceTypeName(getDeviceTypeName(device.getType()));
        dto.setIpAddress(device.getIpAddress());
        dto.setDeviceStatus(device.getStatus());
        dto.setDeviceStatusName(getDeviceStatusName(device.getStatus()));
        dto.setLastHeartbeat(device.getLastHeartbeat());
        dto.setRemark(device.getRemark());
        dto.setCreateTime(device.getCreateTime());
        dto.setUpdateTime(device.getUpdateTime());
        return dto;
    }

    private String getDeviceTypeName(Short deviceType) {
        if (deviceType == null) {
            return "未知";
        }
        switch (deviceType) {
            case DEVICE_TYPE_RFID:
                return "RFID读写器";
            case DEVICE_TYPE_CAMERA:
                return "摄像头";
            case DEVICE_TYPE_ROBOT:
                return "巡检小车";
            default:
                return "未知";
        }
    }

    private String getDeviceStatusName(Short deviceStatus) {
        if (deviceStatus == null) {
            return "未知";
        }
        switch (deviceStatus) {
            case DEVICE_STATUS_OFFLINE:
                return "离线";
            case DEVICE_STATUS_ONLINE:
                return "在线";
            case DEVICE_STATUS_FAULT:
                return "故障";
            default:
                return "未知";
        }
    }

    public Map<String, Object> getConfig(String deviceCode, String version) {
        String latestVersion = "1.0.1";

        Map<String, Object> config = new HashMap<>();
        config.put("version", latestVersion);
        config.put("heartbeatInterval", 1);
        config.put("apiBaseUrl", "/api/device");
        config.put("taskPollInterval", 3);
        config.put("logUploadStrategy", "periodic");
        config.put("networkTimeout", 2000);

        // Add robot specific config
        DeviceCore device = deviceRepository.findByDeviceCode(deviceCode).orElse(null);
        if (device != null && DEVICE_TYPE_ROBOT == device.getType()) {
            robotRepository
                    .findById(device.getDeviceId())
                    .ifPresent(
                            robot -> {
                                if (robot.getMoveSpeedCmS() != null)
                                    config.put("move_speed_cm_s", robot.getMoveSpeedCmS());
                                if (robot.getMotorTrimA() != null)
                                    config.put("motor_trim_a", robot.getMotorTrimA());
                                if (robot.getMotorTrimB() != null)
                                    config.put("motor_trim_b", robot.getMotorTrimB());
                                if (robot.getMotorTrimC() != null)
                                    config.put("motor_trim_c", robot.getMotorTrimC());
                                if (robot.getMotorTrimD() != null)
                                    config.put("motor_trim_d", robot.getMotorTrimD());
                            });

            // If fields are null, provide defaults (based on user's calibration)
            // config.putIfAbsent("move_speed_cm_s", 19.7f);
            // config.putIfAbsent("motor_trim_a", 0.2f);
            // config.putIfAbsent("motor_trim_b", -0.095f);
            // config.putIfAbsent("motor_trim_c", 0.2f);
            // config.putIfAbsent("motor_trim_d", -0.095f);
            config.putIfAbsent("move_speed_cm_s", 19.7f);
            config.putIfAbsent("motor_trim_a", 0.3f);
            config.putIfAbsent("motor_trim_b", -0.195f);
            config.putIfAbsent("motor_trim_c", 0.3f);
            config.putIfAbsent("motor_trim_d", -0.195f);
        }

        return config;
    }

    @Transactional
    public void updateRobotConfig(String deviceCode, Map<String, Object> params) {
        DeviceCore device =
                deviceRepository
                        .findByDeviceCode(deviceCode)
                        .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "设备不存在"));

        if (DEVICE_TYPE_ROBOT != device.getType()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "非机器人设备不支持此配置");
        }

        DeviceInspectionRobot robot =
                robotRepository
                        .findById(device.getDeviceId())
                        .orElseGet(
                                () -> {
                                    DeviceInspectionRobot newRobot = new DeviceInspectionRobot();
                                    newRobot.setDeviceId(device.getDeviceId());
                                    newRobot.setCreateBy(1L);
                                    newRobot.setCreateTime(LocalDateTime.now());
                                    return newRobot;
                                });

        if (params.containsKey("move_speed_cm_s")) {
            robot.setMoveSpeedCmS(toFloat(params.get("move_speed_cm_s")));
        }
        if (params.containsKey("motor_trim_a")) {
            robot.setMotorTrimA(toFloat(params.get("motor_trim_a")));
        }
        if (params.containsKey("motor_trim_b")) {
            robot.setMotorTrimB(toFloat(params.get("motor_trim_b")));
        }
        if (params.containsKey("motor_trim_c")) {
            robot.setMotorTrimC(toFloat(params.get("motor_trim_c")));
        }
        if (params.containsKey("motor_trim_d")) {
            robot.setMotorTrimD(toFloat(params.get("motor_trim_d")));
        }

        robot.setUpdateBy(1L);
        robot.setUpdateTime(LocalDateTime.now());
        robotRepository.save(robot);

        // Push update
        try {
            pushConfigUpdate(deviceCode);
        } catch (Exception e) {
            logger.error("Failed to push config update for device: " + deviceCode, e);
            // Don't fail the transaction just because push failed
        }
    }

    private Float toFloat(Object value) {
        if (value == null) return null;
        if (value instanceof Number) {
            return ((Number) value).floatValue();
        }
        try {
            return Float.parseFloat(value.toString());
        } catch (NumberFormatException e) {
            logger.warn("Invalid float value: {}", value);
            return null;
        }
    }

    public void pushConfigUpdate(String deviceCode) {
        if (deviceConfigPublisher != null) {
            Map<String, Object> config = getConfig(deviceCode, "latest");
            deviceConfigPublisher.publishConfig(deviceCode, config);
        }
    }

    public void uploadLogs(String deviceCode, String logs) {
        if (deviceLogStorage != null) {
            try {
                byte[] bytes = logs.getBytes(java.nio.charset.StandardCharsets.UTF_8);
                java.io.InputStream is = new java.io.ByteArrayInputStream(bytes);
                String fileName = "upload_" + System.currentTimeMillis() + ".log";
                deviceLogStorage.storeLog(deviceCode, fileName, is, bytes.length);
            } catch (Exception e) {
                logger.error("Failed to upload logs for device: " + deviceCode, e);
            }
        } else {
            logger.warn("Device log storage service not available");
        }
    }
}
