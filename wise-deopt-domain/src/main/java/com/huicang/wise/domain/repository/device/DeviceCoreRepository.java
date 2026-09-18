package com.huicang.wise.domain.repository.device;

import com.huicang.wise.domain.device.DeviceCore;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * 设备核心仓储接口
 *
 * @author WiseDepot
 * @version 0.0.26
 * @since 2026-02-27
 */
@Repository
public interface DeviceCoreRepository extends JpaRepository<DeviceCore, Long> {

    /**
     * 根据设备ID查询设备
     *
     * @param deviceId 设备ID
     * @return 设备信息
     */
    Optional<DeviceCore> findByDeviceId(Long deviceId);

    /**
     * 根据设备编码查询设备
     *
     * @param deviceCode 设备编码
     * @return 设备信息
     */
    Optional<DeviceCore> findByDeviceCode(String deviceCode);

    /**
     * 根据设备名称查询设备
     *
     * @param name 设备名称
     * @return 设备信息
     */
    Optional<DeviceCore> findByName(String name);

    /**
     * 根据设备类型查询设备列表
     *
     * @param type 设备类型
     * @return 设备列表
     */
    List<DeviceCore> findByType(Short type);

    /**
     * 根据设备状态查询设备列表
     *
     * @param status 设备状态
     * @return 设备列表
     */
    List<DeviceCore> findByStatus(Short status);

    /**
     * 查询在线设备数量
     *
     * @param status 设备状态
     * @return 设备数量
     */
    long countByStatus(Short status);

    /**
     * 根据设备类型和状态查询设备列表
     *
     * @param type 设备类型
     * @param status 设备状态
     * @return 设备列表
     */
    List<DeviceCore> findByTypeAndStatus(Short type, Short status);

    /**
     * 根据设备ID删除设备
     *
     * @param deviceId 设备ID
     */
    void deleteByDeviceId(Long deviceId);

    /**
     * 检查设备编码是否存在
     *
     * @param deviceCode 设备编码
     * @return 是否存在
     */
    boolean existsByDeviceCode(String deviceCode);
}
