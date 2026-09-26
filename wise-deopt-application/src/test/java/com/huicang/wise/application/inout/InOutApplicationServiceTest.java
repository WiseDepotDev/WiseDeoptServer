package com.huicang.wise.application.inout;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.huicang.wise.common.api.ErrorCode;
import com.huicang.wise.common.exception.BusinessException;
import com.huicang.wise.domain.inout.StockOrder;
import com.huicang.wise.domain.inout.StockOrderDetail;
import com.huicang.wise.domain.inout.StockOrderStatus;
import com.huicang.wise.domain.inout.StockOrderType;
import com.huicang.wise.domain.inventory.Inventory;
import com.huicang.wise.infrastructure.persistence.repository.inout.StockOrderDetailRepository;
import com.huicang.wise.infrastructure.persistence.repository.inout.StockOrderRepository;
import com.huicang.wise.infrastructure.persistence.repository.inventory.InventoryRepository;
import com.huicang.wise.infrastructure.persistence.repository.inventory.ProductRepository;
import com.huicang.wise.infrastructure.persistence.repository.tag.ProductTagRepository;
import com.huicang.wise.infrastructure.persistence.repository.user.UserRepository;
import com.huicang.wise.infrastructure.persistence.repository.warehouse.WarehouseRepository;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * 出入库单状态机与库存变动的应用层单元测试。
 *
 * <p>覆盖 {@code submitStockOrder} / {@code auditStockOrder} / {@code withdrawStockOrder} 的合法与非法状态迁移，
 * 以及审核通过时 {@code processInventory} 对入库累加、出库扣减、库存不足拒绝的判定。所有断言都读取 {@code stockOrderRepository.save()}
 * 捕获到的实体，不依赖 DTO 字段命名。
 */
@ExtendWith(MockitoExtension.class)
class InOutApplicationServiceTest {

    private static final long ORDER_ID = 1001L;
    private static final short INBOUND = 0;
    private static final short OUTBOUND = 1;
    private static final short PENDING = StockOrderStatus.PENDING.getCode().shortValue();
    private static final short SUBMITTED = StockOrderStatus.SUBMITTED.getCode().shortValue();
    private static final short APPROVED = StockOrderStatus.APPROVED.getCode().shortValue();
    private static final short REJECTED = StockOrderStatus.REJECTED.getCode().shortValue();

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

    /** 固化枚举码与 processInventory 内部使用的字面量一致，避免字面量漂移后测试失去意义。 */
    @Test
    @DisplayName("枚举码与入库/出库字面量一致")
    void orderTypeCodesMatchLiterals() {
        assertEquals(INBOUND, StockOrderType.INBOUND.getCode().shortValue());
        assertEquals(OUTBOUND, StockOrderType.OUTBOUND.getCode().shortValue());
    }

    private StockOrder existingOrder(short status, short type) {
        StockOrder order = new StockOrder();
        order.setStatus(status);
        order.setType(type);
        return order;
    }

    private StockOrderDetail detail(Integer quantity) {
        StockOrderDetail item = new StockOrderDetail();
        item.setQuantity(quantity);
        return item;
    }

    private Inventory inventoryOf(int quantity) {
        Inventory inventory = new Inventory();
        inventory.setQuantity(quantity);
        return inventory;
    }

    private void stubFound(StockOrder order) {
        when(stockOrderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));
        // save 是通用夹具：抛异常的路径走不到它，故只对这单个桩放宽必要性校验。
        lenient()
                .when(stockOrderRepository.save(any(StockOrder.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    private List<StockOrderDetail> stubDetails(StockOrderDetail... details) {
        List<StockOrderDetail> list = List.of(details);
        when(stockOrderDetailRepository.findByOrderId(ORDER_ID)).thenReturn(list);
        return list;
    }

    private StockOrder capturedOrder() {
        ArgumentCaptor<StockOrder> captor = ArgumentCaptor.forClass(StockOrder.class);
        verify(stockOrderRepository).save(captor.capture());
        return captor.getValue();
    }

    private Inventory capturedInventory() {
        ArgumentCaptor<Inventory> captor = ArgumentCaptor.forClass(Inventory.class);
        verify(inventoryRepository).save(captor.capture());
        return captor.getValue();
    }

    // ---------- submitStockOrder ----------

    @Test
    @DisplayName("提交：单据不存在抛 NOT_FOUND")
    void submitMissingOrder() {
        when(stockOrderRepository.findById(ORDER_ID)).thenReturn(Optional.empty());

        BusinessException ex =
                assertThrows(BusinessException.class, () -> service.submitStockOrder(ORDER_ID));

        assertEquals(ErrorCode.NOT_FOUND, ex.getErrorCode());
        verify(stockOrderRepository, never()).save(any(StockOrder.class));
    }

    @Test
    @DisplayName("提交：已审核通过的单据不可提交")
    void submitRejectsApprovedOrder() {
        stubFound(existingOrder(APPROVED, INBOUND));

        BusinessException ex =
                assertThrows(BusinessException.class, () -> service.submitStockOrder(ORDER_ID));

        assertEquals(ErrorCode.PARAM_ERROR, ex.getErrorCode());
        verify(stockOrderRepository, never()).save(any(StockOrder.class));
    }

    @Test
    @DisplayName("提交：无明细的单据不可提交")
    void submitRejectsEmptyDetails() {
        stubFound(existingOrder(PENDING, INBOUND));
        stubDetails();

        BusinessException ex =
                assertThrows(BusinessException.class, () -> service.submitStockOrder(ORDER_ID));

        assertEquals(ErrorCode.PARAM_ERROR, ex.getErrorCode());
        verify(stockOrderRepository, never()).save(any(StockOrder.class));
    }

    @Test
    @DisplayName("提交：待处理单据提交后转 SUBMITTED 并记录提交时间")
    void submitPendingOrder() {
        stubFound(existingOrder(PENDING, INBOUND));
        stubDetails(detail(3));

        assertNotNull(service.submitStockOrder(ORDER_ID));

        StockOrder saved = capturedOrder();
        assertEquals(SUBMITTED, saved.getStatus());
        assertNotNull(saved.getSubmitTime());
        assertNotNull(saved.getUpdateTime());
    }

    @Test
    @DisplayName("提交：已驳回单据可再次提交")
    void submitRejectedOrder() {
        stubFound(existingOrder(REJECTED, INBOUND));
        stubDetails(detail(3));

        assertNotNull(service.submitStockOrder(ORDER_ID));

        assertEquals(SUBMITTED, capturedOrder().getStatus());
    }

    // ---------- auditStockOrder ----------

    @Test
    @DisplayName("审核：单据不存在抛 NOT_FOUND")
    void auditMissingOrder() {
        when(stockOrderRepository.findById(ORDER_ID)).thenReturn(Optional.empty());

        BusinessException ex =
                assertThrows(
                        BusinessException.class,
                        () -> service.auditStockOrder(ORDER_ID, true, "ok"));

        assertEquals(ErrorCode.NOT_FOUND, ex.getErrorCode());
    }

    @Test
    @DisplayName("审核：非待审核状态不可审核")
    void auditRejectsPendingOrder() {
        stubFound(existingOrder(PENDING, INBOUND));

        BusinessException ex =
                assertThrows(
                        BusinessException.class,
                        () -> service.auditStockOrder(ORDER_ID, true, "ok"));

        assertEquals(ErrorCode.PARAM_ERROR, ex.getErrorCode());
        verify(stockOrderRepository, never()).save(any(StockOrder.class));
    }

    @Test
    @DisplayName("审核驳回：转 REJECTED、写入原因且不动库存")
    void auditRejectedOrderKeepsInventoryUntouched() {
        stubFound(existingOrder(SUBMITTED, INBOUND));

        assertNotNull(service.auditStockOrder(ORDER_ID, false, "数量不对"));

        StockOrder saved = capturedOrder();
        assertEquals(REJECTED, saved.getStatus());
        assertEquals("数量不对", saved.getRemark());
        verify(inventoryRepository, never()).save(any(Inventory.class));
    }

    @Test
    @DisplayName("审核通过：无明细时直接转 APPROVED")
    void auditApprovedWithoutDetails() {
        stubFound(existingOrder(SUBMITTED, INBOUND));
        when(stockOrderDetailRepository.findByOrderId(ORDER_ID))
                .thenReturn(Collections.emptyList());

        assertNotNull(service.auditStockOrder(ORDER_ID, true, "同意"));

        assertEquals(APPROVED, capturedOrder().getStatus());
        verify(inventoryRepository, never()).save(any(Inventory.class));
    }

    @Test
    @DisplayName("审核通过入库：无库存记录时新建并按数量初始化")
    void auditApprovedInboundCreatesInventory() {
        stubFound(existingOrder(SUBMITTED, INBOUND));
        stubDetails(detail(5));
        when(inventoryRepository.findByWarehouseIdAndProductId(any(), any()))
                .thenReturn(Optional.empty());

        assertNotNull(service.auditStockOrder(ORDER_ID, true, "同意"));

        assertEquals(APPROVED, capturedOrder().getStatus());
        Inventory inventory = capturedInventory();
        assertEquals(5, inventory.getQuantity());
        assertEquals(0, inventory.getLockedQuantity());
        assertNotNull(inventory.getUpdateTime());
    }

    @Test
    @DisplayName("审核通过入库：已有库存时累加")
    void auditApprovedInboundAccumulatesInventory() {
        stubFound(existingOrder(SUBMITTED, INBOUND));
        stubDetails(detail(5));
        when(inventoryRepository.findByWarehouseIdAndProductId(any(), any()))
                .thenReturn(Optional.of(inventoryOf(10)));

        assertNotNull(service.auditStockOrder(ORDER_ID, true, "同意"));

        assertEquals(15, capturedInventory().getQuantity());
    }

    @Test
    @DisplayName("审核通过入库：明细数量为空时按 1 计")
    void auditApprovedInboundDefaultsNullQuantityToOne() {
        stubFound(existingOrder(SUBMITTED, INBOUND));
        stubDetails(detail(null));
        when(inventoryRepository.findByWarehouseIdAndProductId(any(), any()))
                .thenReturn(Optional.empty());

        assertNotNull(service.auditStockOrder(ORDER_ID, true, "同意"));

        assertEquals(1, capturedInventory().getQuantity());
    }

    @Test
    @DisplayName("审核通过出库：无库存记录时拒绝")
    void auditApprovedOutboundWithoutInventoryRejected() {
        stubFound(existingOrder(SUBMITTED, OUTBOUND));
        stubDetails(detail(2));
        when(inventoryRepository.findByWarehouseIdAndProductId(any(), any()))
                .thenReturn(Optional.empty());

        BusinessException ex =
                assertThrows(
                        BusinessException.class,
                        () -> service.auditStockOrder(ORDER_ID, true, "同意"));

        assertEquals(ErrorCode.PARAM_ERROR, ex.getErrorCode());
        verify(inventoryRepository, never()).save(any(Inventory.class));
        verify(stockOrderRepository, never()).save(any(StockOrder.class));
    }

    @Test
    @DisplayName("审核通过出库：库存不足时拒绝")
    void auditApprovedOutboundInsufficientStockRejected() {
        stubFound(existingOrder(SUBMITTED, OUTBOUND));
        stubDetails(detail(5));
        when(inventoryRepository.findByWarehouseIdAndProductId(any(), any()))
                .thenReturn(Optional.of(inventoryOf(3)));

        BusinessException ex =
                assertThrows(
                        BusinessException.class,
                        () -> service.auditStockOrder(ORDER_ID, true, "同意"));

        assertEquals(ErrorCode.PARAM_ERROR, ex.getErrorCode());
        verify(inventoryRepository, never()).save(any(Inventory.class));
    }

    @Test
    @DisplayName("审核通过出库：库存充足时扣减")
    void auditApprovedOutboundDeductsInventory() {
        stubFound(existingOrder(SUBMITTED, OUTBOUND));
        stubDetails(detail(4));
        when(inventoryRepository.findByWarehouseIdAndProductId(any(), any()))
                .thenReturn(Optional.of(inventoryOf(10)));

        assertNotNull(service.auditStockOrder(ORDER_ID, true, "同意"));

        assertEquals(APPROVED, capturedOrder().getStatus());
        assertEquals(6, capturedInventory().getQuantity());
    }

    // ---------- withdrawStockOrder ----------

    @Test
    @DisplayName("撤回：单据不存在抛 NOT_FOUND")
    void withdrawMissingOrder() {
        when(stockOrderRepository.findById(ORDER_ID)).thenReturn(Optional.empty());

        BusinessException ex =
                assertThrows(BusinessException.class, () -> service.withdrawStockOrder(ORDER_ID));

        assertEquals(ErrorCode.NOT_FOUND, ex.getErrorCode());
    }

    @Test
    @DisplayName("撤回：非待审核状态不可撤回")
    void withdrawRejectsPendingOrder() {
        stubFound(existingOrder(PENDING, INBOUND));

        BusinessException ex =
                assertThrows(BusinessException.class, () -> service.withdrawStockOrder(ORDER_ID));

        assertEquals(ErrorCode.PARAM_ERROR, ex.getErrorCode());
        verify(stockOrderRepository, never()).save(any(StockOrder.class));
    }

    @Test
    @DisplayName("撤回：待审核单据退回 PENDING")
    void withdrawSubmittedOrder() {
        stubFound(existingOrder(SUBMITTED, INBOUND));

        assertNotNull(service.withdrawStockOrder(ORDER_ID));

        StockOrder saved = capturedOrder();
        assertEquals(PENDING, saved.getStatus());
        assertNotNull(saved.getUpdateTime());
    }

    // ---------- getStockOrder ----------

    @Test
    @DisplayName("查询：单据不存在抛 NOT_FOUND")
    void getStockOrderMissing() {
        when(stockOrderRepository.findById(ORDER_ID)).thenReturn(Optional.empty());

        BusinessException ex =
                assertThrows(BusinessException.class, () -> service.getStockOrder(ORDER_ID));

        assertEquals(ErrorCode.NOT_FOUND, ex.getErrorCode());
    }

    @Test
    @DisplayName("查询：命中单据返回非空视图")
    void getStockOrderFound() {
        when(stockOrderRepository.findById(ORDER_ID))
                .thenReturn(Optional.of(existingOrder(PENDING, INBOUND)));

        assertNotNull(service.getStockOrder(ORDER_ID));
    }
}
