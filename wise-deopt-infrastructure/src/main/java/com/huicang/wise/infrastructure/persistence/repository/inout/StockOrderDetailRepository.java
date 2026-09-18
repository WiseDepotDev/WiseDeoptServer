package com.huicang.wise.infrastructure.persistence.repository.inout;

import com.huicang.wise.domain.inout.StockOrderDetail;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * 出入库订单明细仓储接口
 *
 * @author WiseDepot
 * @version 0.0.22
 * @since 2026-02-27
 */
@Repository
public interface StockOrderDetailRepository extends JpaRepository<StockOrderDetail, Long> {

    /**
     * 根据订单ID查询订单明细列表
     *
     * @param orderId 订单ID
     * @return 订单明细列表
     */
    List<StockOrderDetail> findByOrderId(Long orderId);

    /**
     * 根据产品ID查询订单明细列表
     *
     * @param productId 产品ID
     * @return 订单明细列表
     */
    List<StockOrderDetail> findByProductId(Long productId);

    /**
     * 根据订单ID删除订单明细
     *
     * @param orderId 订单ID
     */
    void deleteByOrderId(Long orderId);

    /**
     * 检查标签是否有有效的出库单据
     *
     * @param tagId 标签ID
     * @return 有效出库单据明细
     */
    @Query(
            "SELECT sod FROM StockOrderDetail sod "
                    + "JOIN StockOrder so ON sod.orderId = so.orderId "
                    + "WHERE sod.tagId = :tagId "
                    + "AND so.type = 1 "
                    + "AND so.status = 1 "
                    + "ORDER BY so.createTime DESC")
    Optional<StockOrderDetail> findValidOutboundOrderDetailByTagId(@Param("tagId") Long tagId);
}
