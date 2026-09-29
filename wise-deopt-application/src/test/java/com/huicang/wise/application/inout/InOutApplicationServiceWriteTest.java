package com.huicang.wise.application.inout;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.huicang.wise.common.api.ErrorCode;
import com.huicang.wise.common.exception.BusinessException;
import com.huicang.wise.domain.inout.StockOrder;
import com.huicang.wise.domain.inout.StockOrderDetail;
import com.huicang.wise.domain.inout.StockOrderStatus;
import com.huicang.wise.domain.inout.StockOrderType;
import com.huicang.wise.domain.inventory.Inventory;
import com.huicang.wise.domain.tag.ProductTag;
import com.huicang.wise.infrastructure.persistence.repository.inout.StockOrderDetailRepository;
import com.huicang.wise.infrastructure.persistence.repository.inout.StockOrderRepository;
import com.huicang.wise.infrastructure.persistence.repository.inventory.InventoryRepository;
import com.huicang.wise.infrastructure.persistence.repository.inventory.ProductRepository;
import com.huicang.wise.infrastructure.persistence.repository.tag.ProductTagRepository;
import com.huicang.wise.infrastructure.persistence.repository.user.UserRepository;
import com.huicang.wise.infrastructure.persistence.repository.warehouse.WarehouseRepository;
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
 * 出入库单**写入路径**的单元测试（与既有的 {InOutApplicationServiceTest} 互补，后者覆盖状态机与库存变动）。
 *
 * <p>本批钉住两处现状缺陷（只记录、未修）： ① {@code createStockOrder} 用 {@code System.currentTimeMillis()}
 * 在**应用层自造主键** —— 毫秒级分辨率下并发出库单会**撞号**（与 §153 修掉的 {@code createUser} 同族，但此处还需先核对 DDL 是否
 * auto_increment）； ② {@code createStockOrder} 写出的明细 {@code tagId} **恒为 null**（源码注释里那段反复的自我说服即为此）， 而
 * {@code removeItem} 直接 {@code detail.getTagId().equals(...)} ⇒ **只要单里有一条明细就 NPE**。
 */
@ExtendWith(MockitoExtension.class)
class InOutApplicationServiceWriteTest {

    private static final long ORDER_ID = 1001L;
    private static final long WAREHOUSE_ID = 3L;
    private static final long TAG_ID = 55L;
    private static final long PRODUCT_ID = 77L;
    private static final short PENDING = StockOrderStatus.PENDING.getCode().shortValue();
    private static final short REJECTED = StockOrderStatus.REJECTED.getCode().shortValue();
    private static final short SUBMITTED = StockOrderStatus.SUBMITTED.getCode().shortValue();

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

    private StockOrderCreateRequest createRequest() {
        StockOrderCreateRequest request = new StockOrderCreateRequest();
        request.setOrderNo("SO-2026-001");
        request.setWarehouseId(WAREHOUSE_ID);
        request.setCreateBy(9L);
        return request;
    }

    private StockOrderItemCreateRequest createItem(long productId, long tagId, int quantity) {
        StockOrderItemCreateRequest dto = new StockOrderItemCreateRequest();
        dto.setProductId(productId);
        dto.setQuantity(quantity);
        dto.setLocationCode("A-01");
        return dto;
    }

    private StockOrderItemDTO addItemRequest(long productId, long tagId, int quantity) {
        StockOrderItemDTO dto = new StockOrderItemDTO();
        dto.setProductId(productId);
        dto.setTagId(tagId);
        dto.setQuantity(quantity);
        return dto;
    }

    private StockOrder order(short status, int totalItems) {
        StockOrder entity = new StockOrder();
        entity.setOrderId(ORDER_ID);
        entity.setStatus(status);
        entity.setType((short) 0);
        entity.setTotalItems(totalItems);
        entity.setCreateBy(9L);
        entity.setCreateTime(java.time.LocalDateTime.now());
        return entity;
    }

    private void stubOrderSave() {
        when(stockOrderRepository.save(any(StockOrder.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    private StockOrder capturedSavedOrder(int expectedInvocations) {
        ArgumentCaptor<StockOrder> captor = ArgumentCaptor.forClass(StockOrder.class);
        verify(stockOrderRepository, times(expectedInvocations)).save(captor.capture());
        return captor.getAllValues().get(expectedInvocations - 1);
    }

    // ---------------- 创建 ----------------

    @Test
    @DisplayName("建单：单号为空被拒")
    void createRejectsBlankOrderNo() {
        StockOrderCreateRequest request = createRequest();
        request.setOrderNo("  ");

        BusinessException ex =
                assertThrows(BusinessException.class, () -> service.createStockOrder(request));

        assertEquals(ErrorCode.PARAM_ERROR, ex.getErrorCode());
        verify(stockOrderRepository, never()).save(any());
    }

    @Test
    @DisplayName("建单：仓库ID为空被拒")
    void createRejectsNullWarehouse() {
        StockOrderCreateRequest request = createRequest();
        request.setWarehouseId(null);

        BusinessException ex =
                assertThrows(BusinessException.class, () -> service.createStockOrder(request));

        assertEquals(ErrorCode.PARAM_ERROR, ex.getErrorCode());
    }

    @Test
    @DisplayName("建单：仓库不存在抛 NOT_FOUND")
    void createRejectsUnknownWarehouse() {
        when(warehouseRepository.existsById(WAREHOUSE_ID)).thenReturn(false);

        BusinessException ex =
                assertThrows(
                        BusinessException.class, () -> service.createStockOrder(createRequest()));

        assertEquals(ErrorCode.NOT_FOUND, ex.getErrorCode());
        verify(stockOrderRepository, never()).save(any());
    }

    @Test
    @DisplayName("建单：无明细时只存主单，件数为 0")
    void createWithoutItems() {
        when(warehouseRepository.existsById(WAREHOUSE_ID)).thenReturn(true);
        stubOrderSave();

        service.createStockOrder(createRequest());

        StockOrder saved = capturedSavedOrder(1);
        assertEquals("SO-2026-001", saved.getOrderNo());
        assertEquals(WAREHOUSE_ID, saved.getWarehouseId().longValue());
        assertEquals(0, saved.getTotalItems());
        assertEquals(9L, saved.getCreateBy().longValue());
        assertEquals(9L, saved.getUpdateBy().longValue());
        assertNotNull(saved.getCreateTime());
        verify(stockOrderDetailRepository, never()).saveAll(any());
    }

    @Test
    @DisplayName("建单：带明细时批量落明细，并把件数回写到主单")
    void createWithItems() {
        when(warehouseRepository.existsById(WAREHOUSE_ID)).thenReturn(true);
        stubOrderSave();
        StockOrderCreateRequest request = createRequest();
        request.setItems(List.of(createItem(1L, 11L, 5), createItem(2L, 22L, 7)));

        service.createStockOrder(request);

        ArgumentCaptor<List<StockOrderDetail>> captor = ArgumentCaptor.forClass(List.class);
        verify(stockOrderDetailRepository).saveAll(captor.capture());
        List<StockOrderDetail> details = captor.getValue();
        assertEquals(2, details.size());
        assertEquals(1L, details.get(0).getProductId().longValue());
        assertEquals(5, details.get(0).getQuantity());
        assertEquals("A-01", details.get(0).getLocationCode());
        assertEquals(9L, details.get(0).getCreateBy().longValue());

        assertEquals(2, capturedSavedOrder(2).getTotalItems(), "件数应被回写为明细条数");
    }

    @Test
    @DisplayName("建单：类型缺省为入库；状态缺省为待处理")
    void createDefaultsTypeAndStatus() {
        when(warehouseRepository.existsById(WAREHOUSE_ID)).thenReturn(true);
        stubOrderSave();

        service.createStockOrder(createRequest());

        StockOrder saved = capturedSavedOrder(1);
        assertEquals(StockOrderType.INBOUND.getCode().shortValue(), saved.getType());
        assertEquals(PENDING, saved.getStatus());
    }

    @Test
    @DisplayName("建单：类型 OUTBOUND 转成出库码、状态 PROCESSING 转成已审核")
    void createConvertsTypeAndStatus() {
        when(warehouseRepository.existsById(WAREHOUSE_ID)).thenReturn(true);
        stubOrderSave();
        StockOrderCreateRequest request = createRequest();
        request.setType("OUT");
        request.setStatus("PROCESSING");

        service.createStockOrder(request);

        StockOrder saved = capturedSavedOrder(1);
        assertEquals(StockOrderType.OUTBOUND.getCode().shortValue(), saved.getType());
        assertEquals(StockOrderStatus.APPROVED.getCode().shortValue(), saved.getStatus());
    }

    @Test
    @DisplayName("现状缺陷：建单在应用层用 System.currentTimeMillis() 自造主键（落库前已有 ID）")
    void createAssignsPrimaryKeyInApplicationLayer() {
        when(warehouseRepository.existsById(WAREHOUSE_ID)).thenReturn(true);
        stubOrderSave();

        service.createStockOrder(createRequest());

        assertNotNull(
                capturedSavedOrder(1).getOrderId(),
                "现状：orderId 是应用层用 System.currentTimeMillis() 生成的，毫秒内并发会撞号");
    }

    @Test
    @DisplayName("现状缺陷：无法识别的类型字符串静默退化为入库（传 OUTBOUND 会被当成入库）")
    void unknownTypeSilentlyFallsBackToInbound() {
        when(warehouseRepository.existsById(WAREHOUSE_ID)).thenReturn(true);
        stubOrderSave();
        StockOrderCreateRequest request = createRequest();
        request.setType("OUTBOUND");

        service.createStockOrder(request);

        assertEquals(
                StockOrderType.INBOUND.getCode().shortValue(), capturedSavedOrder(1).getType());
        // 实现只识别 IN / OUT 两种写法，其余一律退化为入库 —— 出库单会被静默建成入库单
    }

    // ---------------- 加明细 ----------------

    @Test
    @DisplayName("加明细：单据不存在抛 NOT_FOUND")
    void addItemMissingOrder() {
        when(stockOrderRepository.findById(ORDER_ID)).thenReturn(Optional.empty());

        BusinessException ex =
                assertThrows(
                        BusinessException.class,
                        () -> service.addItem(ORDER_ID, addItemRequest(1L, TAG_ID, 1)));

        assertEquals(ErrorCode.NOT_FOUND, ex.getErrorCode());
        verify(productTagRepository, never()).findById(any());
    }

    @Test
    @DisplayName("加明细：非待处理/已驳回状态被拒（且不查标签）")
    void addItemRejectsWrongStatus() {
        when(stockOrderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order(SUBMITTED, 0)));

        BusinessException ex =
                assertThrows(
                        BusinessException.class,
                        () -> service.addItem(ORDER_ID, addItemRequest(1L, TAG_ID, 1)));

        assertEquals(ErrorCode.PARAM_ERROR, ex.getErrorCode());
        verify(productTagRepository, never()).findById(any());
    }

    @Test
    @DisplayName("加明细：标签不存在抛 NOT_FOUND")
    void addItemMissingTag() {
        when(stockOrderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order(REJECTED, 0)));
        when(productTagRepository.findById(TAG_ID)).thenReturn(Optional.empty());

        BusinessException ex =
                assertThrows(
                        BusinessException.class,
                        () -> service.addItem(ORDER_ID, addItemRequest(1L, TAG_ID, 1)));

        assertEquals(ErrorCode.NOT_FOUND, ex.getErrorCode());
        verify(stockOrderDetailRepository, never()).save(any());
    }

    @Test
    @DisplayName("加明细：产品ID 取自标签、件数 +1")
    void addItemUsesTagProductAndBumpsCount() {
        StockOrder order = order(PENDING, 2);
        when(stockOrderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));
        ProductTag tag = new ProductTag();
        tag.setProductId(PRODUCT_ID);
        when(productTagRepository.findById(TAG_ID)).thenReturn(Optional.of(tag));
        stubOrderSave();

        assertNotNull(service.addItem(ORDER_ID, addItemRequest(999L, TAG_ID, 3)));

        ArgumentCaptor<StockOrderDetail> captor = ArgumentCaptor.forClass(StockOrderDetail.class);
        verify(stockOrderDetailRepository).save(captor.capture());
        assertEquals(PRODUCT_ID, captor.getValue().getProductId().longValue());
        assertEquals(TAG_ID, captor.getValue().getTagId().longValue());
        assertEquals(3, order.getTotalItems());
        verify(stockOrderRepository).save(order);
    }

    // ---------------- 改单 ----------------

    @Test
    @DisplayName("改单：单据不存在抛 NOT_FOUND")
    void updateMissingOrder() {
        when(stockOrderRepository.findById(ORDER_ID)).thenReturn(Optional.empty());

        BusinessException ex =
                assertThrows(
                        BusinessException.class,
                        () -> service.updateStockOrder(ORDER_ID, createRequest()));

        assertEquals(ErrorCode.NOT_FOUND, ex.getErrorCode());
    }

    @Test
    @DisplayName("改单：非待处理/已驳回状态被拒")
    void updateRejectsWrongStatus() {
        when(stockOrderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order(SUBMITTED, 0)));

        BusinessException ex =
                assertThrows(
                        BusinessException.class,
                        () -> service.updateStockOrder(ORDER_ID, createRequest()));

        assertEquals(ErrorCode.PARAM_ERROR, ex.getErrorCode());
        verify(stockOrderRepository, never()).save(any());
    }

    @Test
    @DisplayName("改单：备注为空时保持原备注不变")
    void updateKeepsRemarkWhenNull() {
        StockOrder order = order(PENDING, 0);
        order.setRemark("原备注");
        when(stockOrderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));
        stubOrderSave();

        service.updateStockOrder(ORDER_ID, createRequest());

        assertEquals("原备注", capturedSavedOrder(1).getRemark());
    }

    @Test
    @DisplayName("改单：写入新备注，并把请求里的 createBy 当作操作人")
    void updateAppliesRemarkAndOperator() {
        StockOrder order = order(PENDING, 0);
        when(stockOrderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));
        stubOrderSave();
        StockOrderCreateRequest request = createRequest();
        request.setRemark("补记");

        service.updateStockOrder(ORDER_ID, request);

        StockOrder saved = capturedSavedOrder(1);
        assertEquals("补记", saved.getRemark());
        assertEquals(9L, saved.getUpdateBy().longValue());
        assertNotNull(saved.getUpdateTime());
    }

    // ---------------- 删明细 ----------------

    @Test
    @DisplayName("删明细：单据不存在抛 NOT_FOUND")
    void removeItemMissingOrder() {
        when(stockOrderRepository.findById(ORDER_ID)).thenReturn(Optional.empty());

        BusinessException ex =
                assertThrows(BusinessException.class, () -> service.removeItem(ORDER_ID, TAG_ID));

        assertEquals(ErrorCode.NOT_FOUND, ex.getErrorCode());
    }

    @Test
    @DisplayName("删明细：非待处理/已驳回状态被拒")
    void removeItemRejectsWrongStatus() {
        when(stockOrderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order(SUBMITTED, 1)));

        BusinessException ex =
                assertThrows(BusinessException.class, () -> service.removeItem(ORDER_ID, TAG_ID));

        assertEquals(ErrorCode.PARAM_ERROR, ex.getErrorCode());
        verify(stockOrderDetailRepository, never()).findByOrderId(any());
    }

    @Test
    @DisplayName("删明细：命中则删除并把件数 -1")
    void removeItemDeletesAndDecrements() {
        StockOrder order = order(PENDING, 2);
        StockOrderDetail detail = new StockOrderDetail();
        detail.setTagId(TAG_ID);
        when(stockOrderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));
        when(stockOrderDetailRepository.findByOrderId(ORDER_ID)).thenReturn(List.of(detail));
        stubOrderSave();

        service.removeItem(ORDER_ID, TAG_ID);

        verify(stockOrderDetailRepository).delete(detail);
        assertEquals(1, order.getTotalItems());
        verify(stockOrderRepository).save(order);
    }

    @Test
    @DisplayName("删明细：没有匹配的明细抛 NOT_FOUND 且不落库")
    void removeItemWithoutMatch() {
        StockOrder order = order(PENDING, 1);
        StockOrderDetail detail = new StockOrderDetail();
        detail.setTagId(999L);
        when(stockOrderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));
        when(stockOrderDetailRepository.findByOrderId(ORDER_ID)).thenReturn(List.of(detail));

        BusinessException ex =
                assertThrows(BusinessException.class, () -> service.removeItem(ORDER_ID, TAG_ID));

        assertEquals(ErrorCode.NOT_FOUND, ex.getErrorCode());
        verify(stockOrderDetailRepository, never()).delete(any());
    }

    @Test
    @DisplayName("现状缺陷：明细 tagId 为 null 时删明细直接 NPE（而建单写出的明细正是 null）")
    void removeItemNpeWhenDetailTagIdIsNull() {
        StockOrder order = order(PENDING, 1);
        StockOrderDetail detail = new StockOrderDetail();
        detail.setTagId(null);
        when(stockOrderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));
        when(stockOrderDetailRepository.findByOrderId(ORDER_ID)).thenReturn(List.of(detail));

        assertThrows(
                NullPointerException.class,
                () -> service.removeItem(ORDER_ID, TAG_ID),
                "现状：createStockOrder 写出的明细 tagId 为 null，removeItem 会在此 NPE");
    }

    @Test
    @DisplayName("删明细：只删匹配的那一条，其余保持不动")
    void removeItemOnlyDeletesMatch() {
        StockOrder order = order(PENDING, 2);
        StockOrderDetail keep = new StockOrderDetail();
        keep.setTagId(888L);
        StockOrderDetail target = new StockOrderDetail();
        target.setTagId(TAG_ID);
        when(stockOrderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));
        when(stockOrderDetailRepository.findByOrderId(ORDER_ID)).thenReturn(List.of(keep, target));
        stubOrderSave();

        service.removeItem(ORDER_ID, TAG_ID);

        verify(stockOrderDetailRepository).delete(target);
        verify(stockOrderDetailRepository, never()).delete(keep);
    }

    @Test
    @DisplayName("建单：明细未指定位置时 locationCode 为 null（不编造）")
    void createKeepsNullLocation() {
        when(warehouseRepository.existsById(WAREHOUSE_ID)).thenReturn(true);
        stubOrderSave();
        StockOrderCreateRequest request = createRequest();
        StockOrderItemCreateRequest dto = createItem(1L, 11L, 1);
        dto.setLocationCode(null);
        request.setItems(List.of(dto));

        service.createStockOrder(request);

        ArgumentCaptor<List<StockOrderDetail>> captor = ArgumentCaptor.forClass(List.class);
        verify(stockOrderDetailRepository).saveAll(captor.capture());
        assertNull(captor.getValue().get(0).getLocationCode());
    }

    @Test
    @DisplayName("建单：明细的 tagId 恒为 null（现状，注释里也承认）")
    void createWritesNullTagId() {
        when(warehouseRepository.existsById(WAREHOUSE_ID)).thenReturn(true);
        stubOrderSave();
        StockOrderCreateRequest request = createRequest();
        request.setItems(List.of(createItem(1L, TAG_ID, 1)));

        service.createStockOrder(request);

        ArgumentCaptor<List<StockOrderDetail>> captor = ArgumentCaptor.forClass(List.class);
        verify(stockOrderDetailRepository).saveAll(captor.capture());
        assertNull(captor.getValue().get(0).getTagId(), "现状：即使请求里给了 tagId，落库时也被写成 null");
    }

    @Test
    @DisplayName("建单：一次建单只写一条主单（无明细时不触发第二次 save）")
    void createWritesOrderOnce() {
        when(warehouseRepository.existsById(WAREHOUSE_ID)).thenReturn(true);
        stubOrderSave();

        service.createStockOrder(createRequest());

        verify(stockOrderRepository, times(1)).save(any(StockOrder.class));
        verify(inventoryRepository, never()).save(any(Inventory.class));
    }

    @Test
    @DisplayName("加明细：件数从已有值递增，而不是重置为 1")
    void addItemIncrementsFromExisting() {
        StockOrder order = order(PENDING, 7);
        when(stockOrderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));
        ProductTag tag = new ProductTag();
        tag.setProductId(PRODUCT_ID);
        when(productTagRepository.findById(anyLong())).thenReturn(Optional.of(tag));
        stubOrderSave();

        service.addItem(ORDER_ID, addItemRequest(1L, TAG_ID, 1));

        assertEquals(8, order.getTotalItems());
    }
}
