package com.huicang.wise.infrastructure.repository.inventory;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 类功能描述：产品信息仓储接口
 *
 * @author xingchentye
 * @date 2026-01-19
 * @modified xingchentye 2026-01-19 定义产品基础访问接口
 */
public interface ProductRepository extends JpaRepository<ProductJpaEntity, Long> {
}
