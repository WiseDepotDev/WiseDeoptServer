package com.huicang.wise.infrastructure.repository.inventory;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 类功能描述：库存差异仓储接口
 *
 * @author xingchentye
 * @date 2026-01-25
 */
@Repository
public interface InventoryDifferenceRepository extends JpaRepository<InventoryDifferenceJpaEntity, Long> {

    List<InventoryDifferenceJpaEntity> findByStatus(Integer status);

    List<InventoryDifferenceJpaEntity> findByTaskId(Long taskId);
}
