package com.huicang.wise.domain.repository.tag;

import com.huicang.wise.domain.tag.ProductTag;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * 标签仓储接口
 *
 * @author WiseDepot
 * @version 0.1.12
 * @since 2026-02-27
 */
@Repository
public interface TagRepository extends JpaRepository<ProductTag, Long> {

    /**
     * 根据条形码查询标签
     *
     * @param barcode 条形码
     * @return 标签信息
     */
    Optional<ProductTag> findByBarcode(String barcode);

    /**
     * 根据产品ID查询标签列表
     *
     * @param productId 产品ID
     * @return 标签列表
     */
    List<ProductTag> findAllByProductId(Long productId);

    /**
     * 根据产品ID分页查询标签
     *
     * @param productId 产品ID
     * @param pageable 分页参数
     * @return 标签分页结果
     */
    Page<ProductTag> findByProductId(Long productId, Pageable pageable);

    /**
     * 根据标签状态查询标签列表
     *
     * @param status 标签状态
     * @return 标签列表
     */
    List<ProductTag> findByStatus(Short status);

    /**
     * 根据标签状态分页查询标签
     *
     * @param status 标签状态
     * @param pageable 分页参数
     * @return 标签分页结果
     */
    Page<ProductTag> findByStatus(Short status, Pageable pageable);

    /**
     * 根据产品ID和标签状态查询标签列表
     *
     * @param productId 产品ID
     * @param status 标签状态
     * @return 标签列表
     */
    List<ProductTag> findByProductIdAndStatus(Long productId, Short status);

    /**
     * 根据产品ID和标签状态分页查询标签
     *
     * @param productId 产品ID
     * @param status 标签状态
     * @param pageable 分页参数
     * @return 标签分页结果
     */
    Page<ProductTag> findByProductIdAndStatus(Long productId, Short status, Pageable pageable);

    /**
     * 检查条形码是否存在
     *
     * @param barcode 条形码
     * @return 是否存在
     */
    boolean existsByBarcode(String barcode);

    /**
     * 检查RFID是否存在
     *
     * @param rfid RFID标识
     * @return 是否存在
     */
    boolean existsByRfid(String rfid);

    /**
     * 检查NFC UID是否存在
     *
     * @param nfcUid NFC标识
     * @return 是否存在
     */
    boolean existsByNfcUid(String nfcUid);

    /**
     * 根据产品ID统计标签数量
     *
     * @param productId 产品ID
     * @return 标签数量
     */
    long countByProductId(Long productId);

    /**
     * 根据标签状态统计标签数量
     *
     * @param status 标签状态
     * @return 标签数量
     */
    long countByStatus(Short status);

    /**
     * 综合查询标签列表
     *
     * @param productId 产品ID（可选）
     * @param status 标签状态（可选）
     * @param search 搜索关键词（可选，支持RFID、条码、NFC UID）
     * @param pageable 分页参数
     * @return 标签分页结果
     */
    @Query(
            "SELECT t FROM ProductTag t WHERE "
                    + "(:productId IS NULL OR t.productId = :productId) AND "
                    + "(:status IS NULL OR t.status = :status) AND "
                    + "(:search IS NULL OR t.rfid LIKE %:search% OR t.barcode LIKE %:search% OR t.nfcUid LIKE %:search%)")
    Page<ProductTag> findTags(
            @Param("productId") Long productId,
            @Param("status") Short status,
            @Param("search") String search,
            Pageable pageable);

    /**
     * 根据条形码模糊查询
     *
     * @param barcode 条形码（支持模糊匹配）
     * @param pageable 分页参数
     * @return 标签分页结果
     */
    @Query("SELECT t FROM ProductTag t WHERE t.barcode LIKE %:barcode%")
    Page<ProductTag> findByBarcodeContaining(@Param("barcode") String barcode, Pageable pageable);

    /**
     * 批量查询标签
     *
     * @param barcodes 条形码列表
     * @return 标签列表
     */
    @Query("SELECT t FROM ProductTag t WHERE t.barcode IN :barcodes")
    List<ProductTag> findByBarcodeIn(@Param("barcodes") List<String> barcodes);

    /**
     * 解绑产品标签（将productId设置为null）
     *
     * @param tagId 标签ID
     * @return 更新行数
     */
    @Modifying
    @Query("UPDATE ProductTag t SET t.productId = NULL, t.status = 0 WHERE t.tagId = :tagId")
    int unbindProduct(@Param("tagId") Long tagId);

    /**
     * 批量解绑产品标签
     *
     * @param tagIds 标签ID列表
     * @return 更新行数
     */
    @Modifying
    @Query("UPDATE ProductTag t SET t.productId = NULL, t.status = 0 WHERE t.tagId IN :tagIds")
    int batchUnbindProducts(@Param("tagIds") List<Long> tagIds);

    /**
     * 绑定产品标签
     *
     * @param tagId 标签ID
     * @param productId 产品ID
     * @return 更新行数
     */
    @Modifying
    @Query("UPDATE ProductTag t SET t.productId = :productId, t.status = 1 WHERE t.tagId = :tagId")
    int bindProduct(@Param("tagId") Long tagId, @Param("productId") Long productId);

    /**
     * 批量绑定产品标签
     *
     * @param tagIds 标签ID列表
     * @param productId 产品ID
     * @return 更新行数
     */
    @Modifying
    @Query(
            "UPDATE ProductTag t SET t.productId = :productId, t.status = 1 WHERE t.tagId IN :tagIds")
    int batchBindProducts(@Param("tagIds") List<Long> tagIds, @Param("productId") Long productId);
}
