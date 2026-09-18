package com.huicang.wise.domain.repository.inventory;

import com.huicang.wise.domain.inventory.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * 产品仓储接口
 *
 * @author WiseDepot
 * @version 0.0.26
 * @since 2026-02-27
 */
@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    /**
     * 根据产品名称模糊查询产品
     *
     * @param name 产品名称（支持模糊匹配）
     * @param pageable 分页参数
     * @return 产品分页结果
     */
    @Query("SELECT p FROM Product p WHERE p.name LIKE %:name%")
    Page<Product> findByNameContaining(@Param("name") String name, Pageable pageable);

    /**
     * 根据产品编码查询产品
     *
     * @param code 产品编码
     * @param pageable 分页参数
     * @return 产品分页结果
     */
    @Query("SELECT p FROM Product p WHERE p.code = :code")
    Page<Product> findByCode(@Param("code") String code, Pageable pageable);

    /**
     * 分页查询产品列表
     *
     * @param pageable 分页参数
     * @return 产品分页结果
     */
    Page<Product> findAll(Pageable pageable);

    /**
     * 综合查询产品列表
     *
     * @param name 产品名称（可选）
     * @param pageable 分页参数
     * @return 产品分页结果
     */
    @Query("SELECT p FROM Product p WHERE " + "(:name IS NULL OR p.name LIKE %:name%)")
    Page<Product> findProducts(@Param("name") String name, Pageable pageable);
}
