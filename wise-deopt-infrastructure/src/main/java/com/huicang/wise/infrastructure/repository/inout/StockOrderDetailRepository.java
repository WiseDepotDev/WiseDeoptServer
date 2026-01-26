package com.huicang.wise.infrastructure.repository.inout;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 类功能描述：出入库单详情仓储接口
 *
 * @author xingchentye
 * @date 2026-01-22
 */
@Repository
public interface StockOrderDetailRepository extends JpaRepository<StockOrderDetailJpaEntity, Long> {

    List<StockOrderDetailJpaEntity> findByOrderId(Long orderId);

    List<StockOrderDetailJpaEntity> findByProductId(Long productId);
}
