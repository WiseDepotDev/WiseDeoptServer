package com.huicang.wise.infrastructure.repository.inout;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 类功能描述：出入库单仓储接口
 *
 * @author xingchentye
 * @date 2026-01-19
 * @modified xingchentye 2026-01-19 定义出入库单访问接口
 */
public interface StockOrderRepository extends JpaRepository<StockOrderJpaEntity, Long> {
}

