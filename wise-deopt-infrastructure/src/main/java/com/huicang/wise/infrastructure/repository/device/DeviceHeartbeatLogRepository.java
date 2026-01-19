package com.huicang.wise.infrastructure.repository.device;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface DeviceHeartbeatLogRepository extends JpaRepository<DeviceHeartbeatLogJpaEntity, Long> {

    List<DeviceHeartbeatLogJpaEntity> findByDeviceId(Long deviceId);

    List<DeviceHeartbeatLogJpaEntity> findByDeviceIdAndHeartbeatTimeBetween(Long deviceId,
                                                                           LocalDateTime startTime,
                                                                           LocalDateTime endTime);
}

