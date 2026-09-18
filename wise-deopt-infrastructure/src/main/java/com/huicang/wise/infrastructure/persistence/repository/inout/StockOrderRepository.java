package com.huicang.wise.infrastructure.persistence.repository.inout;

import com.huicang.wise.domain.inout.StockOrder;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * 出入库仓储接口
 *
 * @author WiseDepot
 * @version 0.0.26
 * @since 2026-02-27
 */
@Repository
public interface StockOrderRepository extends JpaRepository<StockOrder, Long> {

    /**
     * 根据单号查询出入库单
     *
     * @param orderNo 单号
     * @return 出入库单
     */
    Optional<StockOrder> findByOrderNo(String orderNo);

    /**
     * 根据仓库ID查询出入库单列表
     *
     * @param warehouseId 仓库ID
     * @return 出入库单列表
     */
    List<StockOrder> findByWarehouseId(Long warehouseId);

    /**
     * 根据仓库ID分页查询出入库单
     *
     * @param warehouseId 仓库ID
     * @param pageable 分页参数
     * @return 出入库单分页结果
     */
    Page<StockOrder> findByWarehouseId(Long warehouseId, Pageable pageable);

    /**
     * 根据单据类型查询出入库单
     *
     * @param type 单据类型
     * @return 出入库单列表
     */
    List<StockOrder> findByType(Short type);

    /**
     * 根据单据类型分页查询出入库单
     *
     * @param type 单据类型
     * @param pageable 分页参数
     * @return 出入库单分页结果
     */
    Page<StockOrder> findByType(Short type, Pageable pageable);

    /**
     * 根据单据状态查询出入库单
     *
     * @param status 单据状态
     * @return 出入库单列表
     */
    List<StockOrder> findByStatus(Short status);

    /**
     * 根据单据状态分页查询出入库单
     *
     * @param status 单据状态
     * @param pageable 分页参数
     * @return 出入库单分页结果
     */
    Page<StockOrder> findByStatus(Short status, Pageable pageable);

    /**
     * 根据创建者ID查询出入库单
     *
     * @param createBy 创建者ID
     * @return 出入库单列表
     */
    List<StockOrder> findByCreateBy(Long createBy);

    /**
     * 根据创建者ID分页查询出入库单
     *
     * @param createBy 创建者ID
     * @param pageable 分页参数
     * @return 出入库单分页结果
     */
    Page<StockOrder> findByCreateBy(Long createBy, Pageable pageable);

    /**
     * 分页查询出入库单列表
     *
     * @param pageable 分页参数
     * @return 出入库单分页结果
     */
    Page<StockOrder> findAll(Pageable pageable);

    /**
     * 根据时间范围查询出入库单
     *
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 出入库单列表
     */
    List<StockOrder> findByCreateTimeBetween(LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 根据时间范围分页查询出入库单
     *
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @param pageable 分页参数
     * @return 出入库单分页结果
     */
    Page<StockOrder> findByCreateTimeBetween(
            LocalDateTime startTime, LocalDateTime endTime, Pageable pageable);

    /**
     * 检查单号是否存在
     *
     * @param orderNo 单号
     * @return 是否存在
     */
    boolean existsByOrderNo(String orderNo);

    /**
     * 根据单据类型统计单据数量
     *
     * @param type 单据类型
     * @return 单据数量
     */
    long countByType(Short type);

    /**
     * 根据单据状态统计单据数量
     *
     * @param status 单据状态
     * @return 单据数量
     */
    long countByStatus(Short status);

    /**
     * 根据单据类型和状态查询出入库单
     *
     * @param type 单据类型
     * @param status 单据状态
     * @return 出入库单列表
     */
    List<StockOrder> findByTypeAndStatus(Short type, Short status);

    /**
     * 根据单据类型和状态分页查询出入库单
     *
     * @param type 单据类型
     * @param status 单据状态
     * @param pageable 分页参数
     * @return 出入库单分页结果
     */
    Page<StockOrder> findByTypeAndStatus(Short type, Short status, Pageable pageable);

    /**
     * 查询待审批的出入库单
     *
     * @param status 订单状态
     * @return 待审批单据列表
     */
    @Query("SELECT o FROM StockOrder o WHERE o.status = :status")
    List<StockOrder> findPendingOrders(@Param("status") Short status);

    /**
     * 查询已审批但未完成的出入库单
     *
     * @param status 订单状态
     * @return 已审批未完成单据列表
     */
    @Query("SELECT o FROM StockOrder o WHERE o.status = :status")
    List<StockOrder> findApprovedButNotCompletedOrders(@Param("status") Short status);
}
