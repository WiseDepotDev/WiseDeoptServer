package com.huicang.wise.application.device;

import com.huicang.wise.common.api.ErrorCode;
import com.huicang.wise.common.exception.BusinessException;
import com.huicang.wise.infrastructure.repository.device.DeviceCameraJpaEntity;
import com.huicang.wise.infrastructure.repository.device.DeviceCameraRepository;
import com.huicang.wise.infrastructure.repository.device.DeviceCoreJpaEntity;
import com.huicang.wise.infrastructure.repository.device.DeviceCoreRepository;
import com.huicang.wise.infrastructure.repository.device.DeviceHeartbeatLogJpaEntity;
import com.huicang.wise.infrastructure.repository.device.DeviceHeartbeatLogRepository;
import com.huicang.wise.infrastructure.repository.device.DeviceInspectionRobotJpaEntity;
import com.huicang.wise.infrastructure.repository.device.DeviceInspectionRobotRepository;
import com.huicang.wise.infrastructure.repository.device.DeviceRfidReaderJpaEntity;
import com.huicang.wise.infrastructure.repository.device.DeviceRfidReaderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class DeviceApplicationService {

    private final DeviceCoreRepository deviceCoreRepository;
    private final DeviceRfidReaderRepository deviceRfidReaderRepository;
    private final DeviceCameraRepository deviceCameraRepository;
    private final DeviceInspectionRobotRepository deviceInspectionRobotRepository;
    private final DeviceHeartbeatLogRepository deviceHeartbeatLogRepository;

    public DeviceApplicationService(DeviceCoreRepository deviceCoreRepository,
                                    DeviceRfidReaderRepository deviceRfidReaderRepository,
                                    DeviceCameraRepository deviceCameraRepository,
                                    DeviceInspectionRobotRepository deviceInspectionRobotRepository,
                                    DeviceHeartbeatLogRepository deviceHeartbeatLogRepository) {
        this.deviceCoreRepository = deviceCoreRepository;
        this.deviceRfidReaderRepository = deviceRfidReaderRepository;
        this.deviceCameraRepository = deviceCameraRepository;
        this.deviceInspectionRobotRepository = deviceInspectionRobotRepository;
        this.deviceHeartbeatLogRepository = deviceHeartbeatLogRepository;
    }

    @Transactional
    public DeviceDTO createDevice(DeviceCreateRequest request) throws BusinessException {
        if (request.getDeviceCode() == null || request.getDeviceCode().isBlank()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "设备编码不能为空");
        }
        if (request.getType() == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "设备类型不能为空");
        }
        DeviceCoreJpaEntity core = new DeviceCoreJpaEntity();
        core.setDeviceId(System.currentTimeMillis());
        core.setName(request.getName());
        core.setDeviceCode(request.getDeviceCode());
        core.setType(request.getType());
        core.setIpAddress(request.getIpAddress());
        core.setStatus(0);
        core.setRemark(request.getRemark());
        core.setCreatedAt(LocalDateTime.now());
        DeviceCoreJpaEntity saved = deviceCoreRepository.save(core);

        if (request.getType() == 0) {
            DeviceRfidReaderJpaEntity rfid = new DeviceRfidReaderJpaEntity();
            rfid.setDeviceId(saved.getDeviceId());
            rfid.setRfidLocation(request.getRfidLocation());
            rfid.setRfidReadRange(request.getRfidReadRange());
            rfid.setCameraLocation(request.getCameraLocation());
            deviceRfidReaderRepository.save(rfid);
        } else if (request.getType() == 1) {
            DeviceCameraJpaEntity camera = new DeviceCameraJpaEntity();
            camera.setDeviceId(saved.getDeviceId());
            camera.setCameraStreamUrl(request.getCameraStreamUrl());
            camera.setCameraLocation(request.getCameraLocation());
            deviceCameraRepository.save(camera);
        } else if (request.getType() == 2) {
            DeviceInspectionRobotJpaEntity robot = new DeviceInspectionRobotJpaEntity();
            robot.setDeviceId(saved.getDeviceId());
            deviceInspectionRobotRepository.save(robot);
        }

        return toDTO(saved);
    }

    @Transactional
    public DeviceDTO updateDevice(DeviceUpdateRequest request) throws BusinessException {
        if (request.getDeviceId() == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "设备ID不能为空");
        }
        DeviceCoreJpaEntity entity = deviceCoreRepository.findById(request.getDeviceId())
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "设备不存在"));
        entity.setName(request.getName());
        entity.setRemark(request.getRemark());
        if (request.getStatus() != null) {
            entity.setStatus(request.getStatus());
        }
        entity.setUpdatedAt(LocalDateTime.now());
        DeviceCoreJpaEntity saved = deviceCoreRepository.save(entity);
        return toDTO(saved);
    }

    public DeviceDTO getDevice(Long deviceId) throws BusinessException {
        DeviceCoreJpaEntity entity = deviceCoreRepository.findById(deviceId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "设备不存在"));
        return toDTO(entity);
    }

    public List<DeviceDTO> listDevices() {
        return deviceCoreRepository.findAll().stream().map(this::toDTO).collect(Collectors.toList());
    }

    public List<DeviceDTO> listDevicesByType(Integer type) {
        return deviceCoreRepository.findByType(type).stream().map(this::toDTO).collect(Collectors.toList());
    }

    public List<DeviceDTO> listDevicesByStatus(Integer status) {
        return deviceCoreRepository.findByStatus(status).stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Transactional
    public void deleteDevice(Long deviceId) throws BusinessException {
        if (deviceCoreRepository.findById(deviceId).isEmpty()) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "设备不存在");
        }
        deviceCoreRepository.deleteById(deviceId);
        deviceRfidReaderRepository.deleteById(deviceId);
        deviceCameraRepository.deleteById(deviceId);
        deviceInspectionRobotRepository.deleteById(deviceId);
    }

    @Transactional
    public void recordHeartbeat(Long deviceId, String remark) throws BusinessException {
        DeviceCoreJpaEntity entity = deviceCoreRepository.findById(deviceId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "设备不存在"));
        LocalDateTime now = LocalDateTime.now();
        entity.setLastHeartbeat(now);
        entity.setStatus(1);
        deviceCoreRepository.save(entity);

        DeviceHeartbeatLogJpaEntity log = new DeviceHeartbeatLogJpaEntity();
        log.setLogId(System.currentTimeMillis());
        log.setDeviceId(deviceId);
        log.setHeartbeatTime(now);
        log.setRemark(remark);
        deviceHeartbeatLogRepository.save(log);
    }

    public List<DeviceHeartbeatLogDTO> listHeartbeatLogs(Long deviceId) {
        return deviceHeartbeatLogRepository.findByDeviceId(deviceId).stream()
                .map(this::toHeartbeatDTO)
                .collect(Collectors.toList());
    }

    public List<DeviceHeartbeatLogDTO> listHeartbeatLogs(Long deviceId,
                                                        LocalDateTime startTime,
                                                        LocalDateTime endTime) {
        return deviceHeartbeatLogRepository.findByDeviceIdAndHeartbeatTimeBetween(deviceId, startTime, endTime)
                .stream()
                .map(this::toHeartbeatDTO)
                .collect(Collectors.toList());
    }

    private DeviceDTO toDTO(DeviceCoreJpaEntity entity) {
        DeviceDTO dto = new DeviceDTO();
        dto.setDeviceId(entity.getDeviceId());
        dto.setName(entity.getName());
        dto.setDeviceCode(entity.getDeviceCode());
        dto.setType(entity.getType());
        dto.setTypeDesc(resolveTypeDesc(entity.getType()));
        dto.setIpAddress(entity.getIpAddress());
        dto.setStatus(entity.getStatus());
        dto.setStatusDesc(resolveStatusDesc(entity.getStatus()));
        dto.setLastHeartbeat(entity.getLastHeartbeat());
        dto.setRemark(entity.getRemark());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setCreatedBy(entity.getCreatedBy());
        dto.setUpdatedAt(entity.getUpdatedAt());
        dto.setUpdatedBy(entity.getUpdatedBy());
        return dto;
    }

    private DeviceHeartbeatLogDTO toHeartbeatDTO(DeviceHeartbeatLogJpaEntity entity) {
        DeviceHeartbeatLogDTO dto = new DeviceHeartbeatLogDTO();
        dto.setLogId(entity.getLogId());
        dto.setDeviceId(entity.getDeviceId());
        dto.setHeartbeatTime(entity.getHeartbeatTime());
        dto.setRemark(entity.getRemark());
        return dto;
    }

    private String resolveTypeDesc(Integer type) {
        if (type == null) {
            return "";
        }
        if (type == 0) {
            return "RFID读写器";
        }
        if (type == 1) {
            return "摄像头";
        }
        if (type == 2) {
            return "巡检小车";
        }
        return "";
    }

    private String resolveStatusDesc(Integer status) {
        if (status == null) {
            return "";
        }
        if (status == 0) {
            return "离线";
        }
        if (status == 1) {
            return "在线";
        }
        if (status == 2) {
            return "故障";
        }
        return "";
    }
}

