package com.huicang.wise.infrastructure.persistence.repository.device;

import com.huicang.wise.domain.device.DeviceInspectionRobot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * 巡检小车设备配置仓储接口
 *
 * @author WiseDepot
 * @version 0.0.23
 * @since 2026-03-14
 */
@Repository
public interface RobotConfigRepository extends JpaRepository<DeviceInspectionRobot, Long> {}
