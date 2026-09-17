package com.huicang.wise.domain.repository.warehouse;

import com.huicang.wise.domain.warehouse.Warehouse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 仓库仓储接口
 *
 * @author WiseDepot
 * @version 0.0.1
 * @since 2026-03-14
 */
@Repository
public interface WarehouseRepository extends JpaRepository<Warehouse, Long> {

    /**
     * 根据关键字查询仓库
     *
     * @param keyword 关键字（名称或编码）
     * @return 仓库列表
     */
    @Query("SELECT w FROM Warehouse w WHERE " +
           "(:keyword IS NULL OR :keyword = '' OR w.warehouseName LIKE %:keyword% OR w.warehouseCode LIKE %:keyword%)")
    List<Warehouse> findByKeyword(@Param("keyword") String keyword);
}
