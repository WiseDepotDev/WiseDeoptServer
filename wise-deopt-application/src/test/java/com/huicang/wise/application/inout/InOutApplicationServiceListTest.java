package com.huicang.wise.application.inout;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.huicang.wise.domain.inout.StockOrder;
import com.huicang.wise.infrastructure.persistence.repository.inout.StockOrderDetailRepository;
import com.huicang.wise.infrastructure.persistence.repository.inout.StockOrderRepository;
import com.huicang.wise.infrastructure.persistence.repository.inventory.InventoryRepository;
import com.huicang.wise.infrastructure.persistence.repository.inventory.ProductRepository;
import com.huicang.wise.infrastructure.persistence.repository.tag.ProductTagRepository;
import com.huicang.wise.infrastructure.persistence.repository.user.UserRepository;
import com.huicang.wise.infrastructure.persistence.repository.warehouse.WarehouseRepository;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

/**
 * 出入库单**列表分页**的补充测试（收口 {@code InOutApplicationService} 剩余的未覆盖行）。
 *
 * <p>钉住两点： ① 分页兜底 —— page &lt; 1 视为第 0 页（0 基）、size &lt; 1 视为 10； ② **本方法的分页是无排序的**（{@code
 * PageRequest.of(page, size)} 不带 Sort）， 与本仓其它列表方法（都按 createTime/createTime 倒序）不一致 ——
 * 即"出入库单列表的顺序由数据库决定"。
 */
@ExtendWith(MockitoExtension.class)
class InOutApplicationServiceListTest {

    @Mock private StockOrderRepository stockOrderRepository;
    @Mock private StockOrderDetailRepository stockOrderDetailRepository;
    @Mock private InventoryRepository inventoryRepository;
    @Mock private ProductTagRepository productTagRepository;
    @Mock private ProductRepository productRepository;
    @Mock private UserRepository userRepository;
    @Mock private WarehouseRepository warehouseRepository;

    private InOutApplicationService service;

    @BeforeEach
    void setUp() {
        service =
                new InOutApplicationService(
                        stockOrderRepository,
                        stockOrderDetailRepository,
                        inventoryRepository,
                        productTagRepository,
                        productRepository,
                        userRepository,
                        warehouseRepository);
    }

    private StockOrder order(String orderNo) {
        StockOrder entity = new StockOrder();
        entity.setOrderId(1001L);
        entity.setOrderNo(orderNo);
        entity.setType((short) 0);
        entity.setStatus((short) 0);
        entity.setTotalItems(1);
        return entity;
    }

    private Pageable capturedPageable() {
        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(stockOrderRepository).findAll(captor.capture());
        return captor.getValue();
    }

    @Test
    @DisplayName("列表：page<1 与 size<1 兜底为第 0 页 10 条，且空页的元数据全为 0")
    void defaultsPagingAndEmptyPageMetadata() {
        when(stockOrderRepository.findAll(any(Pageable.class)))
                .thenReturn(Page.empty(PageRequest.of(0, 10)));

        StockOrderPageDTO dto = service.listStockOrders(0, 0);

        Pageable pageable = capturedPageable();
        assertEquals(0, pageable.getPageNumber());
        assertEquals(10, pageable.getPageSize());
        assertTrue(dto.getContent().isEmpty());
        assertEquals(0L, dto.getTotalElements());
        assertEquals(0, dto.getNumber());
        assertEquals(10, dto.getSize());
        assertEquals(0, dto.getTotalPages());
    }

    @Test
    @DisplayName("列表：页码 1 基转 0 基，且**不带排序**（与其它列表方法不同）")
    void convertsPageNumberAndIsUnsorted() {
        when(stockOrderRepository.findAll(any(Pageable.class))).thenReturn(Page.empty());

        service.listStockOrders(3, 25);

        Pageable pageable = capturedPageable();
        assertEquals(2, pageable.getPageNumber());
        assertEquals(25, pageable.getPageSize());
        assertTrue(
                pageable.getSort().isUnsorted(), "现状：本方法未指定排序，顺序由数据库返回决定（其它列表方法都按 createTime 倒序）");
    }

    @Test
    @DisplayName("列表：内容逐条映射，并回填总数/页数/页码/页大小")
    void mapsContentAndMetadata() {
        when(stockOrderRepository.findAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(order("SO-1")), PageRequest.of(0, 10), 25));

        StockOrderPageDTO dto = service.listStockOrders(1, 10);

        assertEquals(1, dto.getContent().size());
        assertEquals(25L, dto.getTotalElements());
        assertEquals(3, dto.getTotalPages());
        assertEquals(0, dto.getNumber());
        assertEquals(10, dto.getSize());
    }
}
