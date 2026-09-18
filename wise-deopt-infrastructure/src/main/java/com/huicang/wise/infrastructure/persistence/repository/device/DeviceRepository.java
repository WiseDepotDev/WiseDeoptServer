package com.huicang.wise.infrastructure.persistence.repository.device;

import com.huicang.wise.domain.device.DeviceCore;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * 设备仓储接口
 *
 * @author WiseDepot
 * @version 0.0.26
 * @since 2026-02-27
 */
@Repository
public interface DeviceRepository extends JpaRepository<DeviceCore, Long> {

    /**
     * 根据设备编码查询设备
     *
     * @param deviceCode 设备编码
     * @return 设备信息
     */
    Optional<DeviceCore> findByDeviceCode(String deviceCode);

    /**
     * 根据IP地址查询设备
     *
     * @param ipAddress IP地址
     * @return 设备信息
     */
    Optional<DeviceCore> findByIpAddress(String ipAddress);

    /**
     * 根据设备名称模糊查询设备
     *
     * @param deviceName 设备名称（支持模糊匹配）
     * @param pageable 分页参数
     * @return 设备分页结果
     */
    @Query("SELECT d FROM DeviceCore d WHERE d.name LIKE %:deviceName%")
    Page<DeviceCore> findByDeviceNameContaining(
            @Param("deviceName") String deviceName, Pageable pageable);

    /**
     * 根据设备类型查询设备
     *
     * @param type 设备类型
     * @return 设备列表
     */
    List<DeviceCore> findByType(Short type);

    /**
     * 根据设备类型分页查询设备
     *
     * @param type 设备类型
     * @param pageable 分页参数
     * @return 设备分页结果
     */
    Page<DeviceCore> findByType(Short type, Pageable pageable);

    /**
     * 根据关键字、类型、状态查询设备
     *
     * @param keyword 关键字（名称或编码）
     * @param type 设备类型
     * @param status 设备状态
     * @return 设备列表
     */
    @Query(
            "SELECT d FROM DeviceCore d WHERE "
                    + "(:keyword IS NULL OR :keyword = '' OR d.name LIKE CONCAT('%', :keyword, '%') OR d.deviceCode LIKE CONCAT('%', :keyword, '%')) AND "
                    + "(:type IS NULL OR d.type = :type) AND "
                    + "(:status IS NULL OR d.status = :status)")
    List<DeviceCore> findByKeywordAndTypeAndStatus(
            @Param("keyword") String keyword,
            @Param("type") Short type,
            @Param("status") Short status);

    /**
     * 根据设备状态查询设备
     *
     * @param status 设备状态
     * @return 设备列表
     */
    List<DeviceCore> findByStatus(Short status);

    /**
     * 根据设备状态分页查询设备
     *
     * @param status 设备状态
     * @param pageable 分页参数
     * @return 设备分页结果
     */
    Page<DeviceCore> findByStatus(Short status, Pageable pageable);

    /**
     * 分页查询设备列表
     *
     * @param pageable 分页参数
     * @return 设备分页结果
     */
    Page<DeviceCore> findAll(Pageable pageable);

    /**
     * 检查设备编码是否存在
     *
     * @param deviceCode 设备编码
     * @return 是否存在
     */
    boolean existsByDeviceCode(String deviceCode);

    /**
     * 根据设备类型统计设备数量
     *
     * @param type 设备类型
     * @return 设备数量
     */
    long countByType(Short type);

    /**
     * 根据设备状态统计设备数量
     *
     * @param status 设备状态
     * @return 设备数量
     */
    long countByStatus(Short status);

    /**
     * 查询离线设备
     *
     * @param lastHeartbeatTime 最后心跳时间阈值
     * @return 离线设备列表
     */
    @Query("SELECT d FROM DeviceCore d WHERE d.lastHeartbeat < :lastHeartbeatTime")
    List<DeviceCore> findOfflineDevices(
            @Param("lastHeartbeatTime") java.time.LocalDateTime lastHeartbeatTime);

    /**
     * 根据设备类型和状态查询设备
     *
     * @param type 设备类型
     * @param status 设备状态
     * @return 设备列表
     */
    List<DeviceCore> findByTypeAndStatus(Short type, Short status);

    /**
     * 统计启用设备数量
     *
     * @param status 状态值
     * @return 启用设备数量
     */
    @Query(value = "SELECT COUNT(*) FROM device_core WHERE status != :status", nativeQuery = true)
    long countEnabledDevices(@Param("status") Short status);
}
