package com.huicang.wise.infrastructure.persistence.repository.inventory;

import com.huicang.wise.domain.inventory.Inventory;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * 库存仓储接口
 *
 * @author WiseDepot
 * @version 0.0.26
 * @since 2026-02-27
 */
@Repository
public interface InventoryRepository extends JpaRepository<Inventory, Long> {

    /**
     * 根据仓库ID和产品ID查询库存
     *
     * @param warehouseId 仓库ID
     * @param productId 产品ID
     * @return 库存记录
     */
    Optional<Inventory> findByWarehouseIdAndProductId(Long warehouseId, Long productId);

    /**
     * 根据仓库ID查询库存列表
     *
     * @param warehouseId 仓库ID
     * @return 库存列表
     */
    List<Inventory> findByWarehouseId(Long warehouseId);

    /**
     * 根据产品ID查询库存列表
     *
     * @param productId 产品ID
     * @return 库存列表
     */
    List<Inventory> findAllByProductId(Long productId);

    /**
     * 根据产品ID分页查询库存
     *
     * @param productId 产品ID
     * @param pageable 分页参数
     * @return 库存分页结果
     */
    Page<Inventory> findByProductId(Long productId, Pageable pageable);

    /**
     * 根据仓库ID分页查询库存
     *
     * @param warehouseId 仓库ID
     * @param pageable 分页参数
     * @return 库存分页结果
     */
    Page<Inventory> findByWarehouseId(Long warehouseId, Pageable pageable);

    /**
     * 根据产品ID和仓库ID分页查询库存
     *
     * @param productId 产品ID
     * @param warehouseId 仓库ID
     * @param pageable 分页参数
     * @return 库存分页结果
     */
    Page<Inventory> findByProductIdAndWarehouseId(
            Long productId, Long warehouseId, Pageable pageable);

    /**
     * 查询低库存预警
     *
     * @param threshold 阈值
     * @return 低库存列表
     */
    @Query("SELECT i FROM Inventory i WHERE i.quantity <= :threshold")
    List<Inventory> findLowStock(@Param("threshold") Integer threshold);

    /**
     * 统计产品总库存
     *
     * @param productId 产品ID
     * @return 总库存数量
     */
    @Query(
            value =
                    "SELECT COALESCE(SUM(quantity), 0) FROM inventory WHERE product_id = :productId",
            nativeQuery = true)
    Integer sumQuantityByProductId(@Param("productId") Long productId);

    /**
     * 统计产品总锁定数量
     *
     * @param productId 产品ID
     * @return 总锁定数量
     */
    @Query(
            value =
                    "SELECT COALESCE(SUM(locked_quantity), 0) FROM inventory WHERE product_id = :productId",
            nativeQuery = true)
    Integer sumLockedQuantityByProductId(@Param("productId") Long productId);

    /**
     * 统计所有产品库存总量
     *
     * @return 库存总量
     */
    @Query(value = "SELECT COALESCE(SUM(quantity), 0) FROM inventory", nativeQuery = true)
    Integer sumTotalQuantity();

    /**
     * 按产品ID统计库存
     *
     * @return 产品库存统计列表
     */
    @Query(
            value = "SELECT product_id, SUM(quantity) as total FROM inventory GROUP BY product_id",
            nativeQuery = true)
    List<Object[]> countByProductGroup();
}
