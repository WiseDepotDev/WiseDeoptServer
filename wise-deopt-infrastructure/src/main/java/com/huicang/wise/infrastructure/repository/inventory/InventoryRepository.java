package com.huicang.wise.infrastructure.repository.inventory;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * 类功能描述：库存明细仓储接口
 *
 * @author xingchentye
 * @date 2026-01-19
 * @modified xingchentye 2026-01-19 定义库存查询接口
 */
public interface InventoryRepository extends JpaRepository<InventoryJpaEntity, Long> {

    /**
     * 方法功能描述：根据产品ID查询库存明细
     *
     * @param productId 产品主键ID
     * @return 库存明细列表
     */
    List<InventoryJpaEntity> findByProductId(Long productId);
}
