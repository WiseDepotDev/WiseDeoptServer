package com.huicang.wise.domain.repository.tag;

import com.huicang.wise.domain.tag.ProductTag;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * 产品标签仓储接口
 *
 * @author WiseDepot
 * @version 0.0.22
 * @since 2026-02-27
 */
@Repository
public interface ProductTagRepository extends JpaRepository<ProductTag, Long> {

    /**
     * 根据条形码查询标签信息
     *
     * @param barcode 条形码
     * @return 标签信息
     */
    Optional<ProductTag> findByBarcode(String barcode);

    /**
     * 根据RFID查询标签信息
     *
     * @param rfid RFID标识
     * @return 标签信息
     */
    Optional<ProductTag> findByRfid(String rfid);

    /**
     * 根据产品ID查询标签列表
     *
     * @param productId 产品ID
     * @return 标签列表
     */
    List<ProductTag> findByProductId(Long productId);

    /**
     * 根据标签状态查询标签列表
     *
     * @param status 标签状态
     * @return 标签列表
     */
    List<ProductTag> findByStatus(Short status);

    /**
     * 根据产品ID和状态查询标签列表
     *
     * @param productId 产品ID
     * @param status 标签状态
     * @return 标签列表
     */
    List<ProductTag> findByProductIdAndStatus(Long productId, Short status);

    /**
     * 根据RFID列表批量查询标签
     *
     * @param rfids RFID标识列表
     * @return 标签列表
     */
    @Query("SELECT t FROM ProductTag t WHERE t.rfid IN :rfids")
    List<ProductTag> findByRfidIn(@Param("rfids") List<String> rfids);
}
