package com.huicang.wise.application.tag;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.huicang.wise.application.captcha.CaptchaApplicationService;
import com.huicang.wise.common.api.ErrorCode;
import com.huicang.wise.common.exception.BusinessException;
import com.huicang.wise.domain.inventory.Product;
import com.huicang.wise.domain.tag.ProductTag;
import com.huicang.wise.infrastructure.persistence.repository.inventory.ProductRepository;
import com.huicang.wise.infrastructure.persistence.repository.tag.TagRepository;
import java.util.List;
import java.util.Optional;
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
 * 标签应用服务的单元测试：CRUD 与唯一性校验、分页与筛选、绑定/解绑与批量操作、验证码前置校验。
 *
 * <p>本批把一条**现状不一致**钉住：{@code listTagsByProduct} 的排序属性写成 {@code createdAt}， 而本类其余列表方法与实体字段都是 {@code
 * createTime}。单测里仓储被 mock，所以这不会"失败"， 但断言把传下去的属性名固定住了 —— 一旦有人按实体字段纠正它，该断言会失败，从而迫使改动被显式确认。
 *
 * <p>另一条重点：{@code batchBindTagsWithCaptcha} 的验证码校验必须发生在**任何仓储操作之前**， 否则"验证码"就只是走过场。本批用 {@code
 * verifyNoInteractions(tagRepository)} 把这条安全约束钉住。
 */
@ExtendWith(MockitoExtension.class)
class TagApplicationServiceTest {

    private static final long TAG_ID = 5L;
    private static final long PRODUCT_ID = 7L;

    @Mock private TagRepository tagRepository;
    @Mock private ProductRepository productRepository;
    @Mock private CaptchaApplicationService captchaApplicationService;

    private TagApplicationService service;

    @BeforeEach
    void setUp() {
        service =
                new TagApplicationService(
                        tagRepository, productRepository, captchaApplicationService);
    }

    private ProductTag tag(String barcode, String nfcUid, String rfid) {
        ProductTag entity = new ProductTag();
        entity.setBarcode(barcode);
        entity.setNfcUid(nfcUid);
        entity.setRfid(rfid);
        return entity;
    }

    private Product product(String name) {
        Product entity = new Product();
        entity.setName(name);
        entity.setCode("P1");
        return entity;
    }

    private ProductTagCreateRequest createRequest(String barcode) {
        ProductTagCreateRequest request = new ProductTagCreateRequest();
        request.setBarcode(barcode);
        return request;
    }

    private ProductTag capturedTag() {
        ArgumentCaptor<ProductTag> captor = ArgumentCaptor.forClass(ProductTag.class);
        verify(tagRepository).save(captor.capture());
        return captor.getValue();
    }

    // ---------------- 创建 ----------------

    @Test
    @DisplayName("创建标签：三项标识全空被拒")
    void createTagRejectsAllIdentifiersBlank() {
        BusinessException ex =
                assertThrows(
                        BusinessException.class, () -> service.createTag(createRequest("   ")));

        assertEquals(ErrorCode.PARAM_ERROR, ex.getErrorCode());
        verify(tagRepository, never()).save(any(ProductTag.class));
    }

    @Test
    @DisplayName("创建标签：条形码重复被拒")
    void createTagRejectsDuplicateBarcode() {
        ProductTagCreateRequest request = createRequest("BC1");
        when(tagRepository.existsByBarcode("BC1")).thenReturn(true);

        BusinessException ex =
                assertThrows(BusinessException.class, () -> service.createTag(request));

        assertEquals(ErrorCode.PARAM_ERROR, ex.getErrorCode());
    }

    @Test
    @DisplayName("创建标签：RFID 重复被拒")
    void createTagRejectsDuplicateRfid() {
        ProductTagCreateRequest request = createRequest(null);
        request.setRfid("RF1");
        when(tagRepository.existsByRfid("RF1")).thenReturn(true);

        BusinessException ex =
                assertThrows(BusinessException.class, () -> service.createTag(request));

        assertEquals(ErrorCode.PARAM_ERROR, ex.getErrorCode());
    }

    @Test
    @DisplayName("创建标签：NFC 重复被拒")
    void createTagRejectsDuplicateNfc() {
        ProductTagCreateRequest request = createRequest(null);
        request.setNfcUid("NFC1");
        when(tagRepository.existsByNfcUid("NFC1")).thenReturn(true);

        BusinessException ex =
                assertThrows(BusinessException.class, () -> service.createTag(request));

        assertEquals(ErrorCode.PARAM_ERROR, ex.getErrorCode());
    }

    @Test
    @DisplayName("创建标签：指定产品但产品不存在抛 NOT_FOUND")
    void createTagRejectsMissingProduct() {
        ProductTagCreateRequest request = createRequest("BC1");
        request.setProductId(PRODUCT_ID);
        when(tagRepository.existsByBarcode("BC1")).thenReturn(false);
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.empty());

        BusinessException ex =
                assertThrows(BusinessException.class, () -> service.createTag(request));

        assertEquals(ErrorCode.NOT_FOUND, ex.getErrorCode());
        verify(tagRepository, never()).save(any(ProductTag.class));
    }

    @Test
    @DisplayName("创建标签：空白标识归一为 null，状态缺省 0，审计字段被填充")
    void createTagNormalisesBlankIdentifiers() {
        ProductTagCreateRequest request = createRequest("BC1");
        request.setNfcUid("   ");
        request.setRfid(null);
        when(tagRepository.existsByBarcode("BC1")).thenReturn(false);
        when(tagRepository.save(any(ProductTag.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        assertNotNull(service.createTag(request));

        ProductTag saved = capturedTag();
        assertEquals("BC1", saved.getBarcode());
        assertNull(saved.getNfcUid());
        assertNull(saved.getRfid());
        assertNull(saved.getProductId());
        assertEquals((short) 0, saved.getStatus());
        assertEquals(1L, saved.getCreateBy());
        assertNotNull(saved.getCreateTime());
        assertNotNull(saved.getUpdateTime());
    }

    // ---------------- 更新 ----------------

    @Test
    @DisplayName("更新标签：标签不存在抛 NOT_FOUND")
    void updateTagMissing() {
        when(tagRepository.findById(TAG_ID)).thenReturn(Optional.empty());

        BusinessException ex =
                assertThrows(
                        BusinessException.class,
                        () -> service.updateTag(TAG_ID, new ProductTagUpdateRequest()));

        assertEquals(ErrorCode.NOT_FOUND, ex.getErrorCode());
    }

    @Test
    @DisplayName("更新标签：换绑到不存在的产品抛 NOT_FOUND")
    void updateTagRejectsMissingProduct() {
        when(tagRepository.findById(TAG_ID)).thenReturn(Optional.of(tag("BC1", null, null)));
        ProductTagUpdateRequest request = new ProductTagUpdateRequest();
        request.setProductId(PRODUCT_ID);
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.empty());

        BusinessException ex =
                assertThrows(BusinessException.class, () -> service.updateTag(TAG_ID, request));

        assertEquals(ErrorCode.NOT_FOUND, ex.getErrorCode());
        verify(tagRepository, never()).save(any(ProductTag.class));
    }

    @Test
    @DisplayName("更新标签：条形码改成别的已存在条码被拒")
    void updateTagRejectsDuplicateBarcode() {
        when(tagRepository.findById(TAG_ID)).thenReturn(Optional.of(tag("BC1", null, null)));
        ProductTagUpdateRequest request = new ProductTagUpdateRequest();
        request.setBarcode("BC2");
        when(tagRepository.existsByBarcode("BC2")).thenReturn(true);

        BusinessException ex =
                assertThrows(BusinessException.class, () -> service.updateTag(TAG_ID, request));

        assertEquals(ErrorCode.PARAM_ERROR, ex.getErrorCode());
    }

    @Test
    @DisplayName("更新标签：条码与本标签相同则不做重复校验")
    void updateTagSkipsUniquenessCheckForSameBarcode() {
        when(tagRepository.findById(TAG_ID)).thenReturn(Optional.of(tag("BC1", null, null)));
        ProductTagUpdateRequest request = new ProductTagUpdateRequest();
        request.setBarcode("BC1");
        when(tagRepository.save(any(ProductTag.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.updateTag(TAG_ID, request);

        verify(tagRepository, never()).existsByBarcode(any());
        assertEquals("BC1", capturedTag().getBarcode());
    }

    @Test
    @DisplayName("更新标签：空条码清空为 null")
    void updateTagClearsBarcodeWhenBlank() {
        when(tagRepository.findById(TAG_ID)).thenReturn(Optional.of(tag("BC1", null, null)));
        ProductTagUpdateRequest request = new ProductTagUpdateRequest();
        request.setBarcode("  ");
        when(tagRepository.save(any(ProductTag.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.updateTag(TAG_ID, request);

        assertNull(capturedTag().getBarcode());
    }

    @Test
    @DisplayName("更新标签：状态、NFC、RFID 被写入")
    void updateTagWritesStatusAndIdentifiers() {
        when(tagRepository.findById(TAG_ID)).thenReturn(Optional.of(tag("BC1", null, null)));
        ProductTagUpdateRequest request = new ProductTagUpdateRequest();
        request.setStatus((short) 2);
        request.setNfcUid("NFC9");
        request.setRfid("RF9");
        when(tagRepository.save(any(ProductTag.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.updateTag(TAG_ID, request);

        ProductTag saved = capturedTag();
        assertEquals((short) 2, saved.getStatus());
        assertEquals("NFC9", saved.getNfcUid());
        assertEquals("RF9", saved.getRfid());
        assertNotNull(saved.getUpdateTime());
    }

    // ---------------- 删除 / 详情 ----------------

    @Test
    @DisplayName("删除标签：不存在抛 NOT_FOUND")
    void deleteTagMissing() {
        when(tagRepository.existsById(TAG_ID)).thenReturn(false);

        BusinessException ex =
                assertThrows(BusinessException.class, () -> service.deleteTag(TAG_ID));

        assertEquals(ErrorCode.NOT_FOUND, ex.getErrorCode());
        verify(tagRepository, never()).deleteById(any());
    }

    @Test
    @DisplayName("删除标签：存在则按 ID 删除")
    void deleteTagSuccess() {
        when(tagRepository.existsById(TAG_ID)).thenReturn(true);

        service.deleteTag(TAG_ID);

        verify(tagRepository).deleteById(TAG_ID);
    }

    @Test
    @DisplayName("标签详情：不存在抛 NOT_FOUND")
    void getTagMissing() {
        when(tagRepository.findById(TAG_ID)).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class, () -> service.getTag(TAG_ID));

        assertEquals(ErrorCode.NOT_FOUND, ex.getErrorCode());
    }

    @Test
    @DisplayName("标签详情：带产品时补齐产品名称")
    void getTagEnrichesProductName() {
        ProductTag entity = tag("BC1", null, null);
        entity.setProductId(PRODUCT_ID);
        when(tagRepository.findById(TAG_ID)).thenReturn(Optional.of(entity));
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(product("螺丝")));

        ProductTagDTO dto = service.getTag(TAG_ID);

        assertEquals("螺丝", dto.getProductName());
        assertEquals("BC1", dto.getBarcode());
    }

    @Test
    @DisplayName("按条码查标签：不存在抛 NOT_FOUND")
    void getTagByCodeMissing() {
        when(tagRepository.findByBarcode("BC1")).thenReturn(Optional.empty());

        BusinessException ex =
                assertThrows(BusinessException.class, () -> service.getTagByCode("BC1"));

        assertEquals(ErrorCode.NOT_FOUND, ex.getErrorCode());
    }

    @Test
    @DisplayName("按条码查标签：命中返回")
    void getTagByCodeFound() {
        when(tagRepository.findByBarcode("BC1")).thenReturn(Optional.of(tag("BC1", null, null)));

        assertEquals("BC1", service.getTagByCode("BC1").getBarcode());
    }

    // ---------------- 列表 ----------------

    @Test
    @DisplayName("标签列表：分页兜底并按创建时间倒序")
    void listTagsDefaultsPaging() {
        when(tagRepository.findTags(any(), any(), any(), any())).thenReturn(Page.empty());

        service.listTags(null, null, null, 0, 0);

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(tagRepository).findTags(any(), any(), any(), captor.capture());
        assertEquals(0, captor.getValue().getPageNumber());
        assertEquals(10, captor.getValue().getPageSize());
        assertNotNull(captor.getValue().getSort().getOrderFor("createTime"));
    }

    @Test
    @DisplayName("标签列表：状态字符串解析为 Short 透传")
    void listTagsParsesStatus() {
        when(tagRepository.findTags(any(), any(), any(), any())).thenReturn(Page.empty());

        service.listTags(null, "1", null, 1, 10);

        ArgumentCaptor<Short> captor = ArgumentCaptor.forClass(Short.class);
        verify(tagRepository).findTags(any(), captor.capture(), any(), any());
        assertEquals((short) 1, captor.getValue());
    }

    @Test
    @DisplayName("标签列表：非法状态被吞成 null（退化为不筛选）")
    void listTagsSwallowsInvalidStatus() {
        when(tagRepository.findTags(any(), any(), any(), any())).thenReturn(Page.empty());

        service.listTags(null, "启用", null, 1, 10);

        ArgumentCaptor<Short> captor = ArgumentCaptor.forClass(Short.class);
        verify(tagRepository).findTags(any(), captor.capture(), any(), any());
        assertNull(captor.getValue());
    }

    @Test
    @DisplayName("标签列表：映射结果行")
    void listTagsMapsRows() {
        when(tagRepository.findTags(any(), any(), any(), any()))
                .thenReturn(
                        new PageImpl<>(List.of(tag("BC1", null, null)), PageRequest.of(0, 10), 1));

        ProductTagPageDTO page = service.listTags(null, null, null, 1, 10);

        assertEquals(1L, page.getTotal());
        assertEquals("BC1", page.getRows().get(0).getBarcode());
    }

    @Test
    @DisplayName("按产品查标签：现状不一致 —— 排序属性是 createdAt 而非 createTime")
    void listTagsByProductSortsByInconsistentProperty() {
        when(tagRepository.findByProductId(any(), any())).thenReturn(Page.empty());

        service.listTagsByProduct(PRODUCT_ID, 0, 0);

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(tagRepository).findByProductId(any(), captor.capture());
        assertEquals(0, captor.getValue().getPageNumber());
        assertEquals(10, captor.getValue().getPageSize());
        assertNotNull(
                captor.getValue().getSort().getOrderFor("createdAt"),
                "现状：这里的排序属性是 createdAt，与本类其它方法与实体字段 createTime 不一致");
        assertNull(captor.getValue().getSort().getOrderFor("createTime"));
    }

    @Test
    @DisplayName("搜索标签：分页兜底并映射")
    void searchTagsDefaultsPaging() {
        when(tagRepository.findByBarcodeContaining(any(), any()))
                .thenReturn(
                        new PageImpl<>(List.of(tag("BC1", null, null)), PageRequest.of(0, 10), 1));

        ProductTagPageDTO page = service.searchTags("BC", 0, 0);

        assertEquals(1L, page.getTotal());
        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(tagRepository).findByBarcodeContaining(any(), captor.capture());
        assertEquals(10, captor.getValue().getPageSize());
    }

    @Test
    @DisplayName("批量按条码查标签：映射列表")
    void batchGetTagsMapsAll() {
        when(tagRepository.findByBarcodeIn(List.of("BC1", "BC2")))
                .thenReturn(List.of(tag("BC1", null, null), tag("BC2", null, null)));

        assertEquals(2, service.batchGetTags(List.of("BC1", "BC2")).size());
    }

    // ---------------- 绑定 / 解绑 ----------------

    @Test
    @DisplayName("绑定标签：标签不存在抛 NOT_FOUND")
    void bindTagMissingTag() {
        when(tagRepository.existsById(TAG_ID)).thenReturn(false);

        BusinessException ex =
                assertThrows(BusinessException.class, () -> service.bindTag(TAG_ID, PRODUCT_ID));

        assertEquals(ErrorCode.NOT_FOUND, ex.getErrorCode());
        verifyNoInteractions(productRepository);
    }

    @Test
    @DisplayName("绑定标签：产品不存在抛 NOT_FOUND")
    void bindTagMissingProduct() {
        when(tagRepository.existsById(TAG_ID)).thenReturn(true);
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.empty());

        BusinessException ex =
                assertThrows(BusinessException.class, () -> service.bindTag(TAG_ID, PRODUCT_ID));

        assertEquals(ErrorCode.NOT_FOUND, ex.getErrorCode());
        verify(tagRepository, never()).bindProduct(any(), any());
    }

    @Test
    @DisplayName("绑定标签：更新行数为 0 时报 PARAM_ERROR（绑定失败）")
    void bindTagNoRowUpdated() {
        when(tagRepository.existsById(TAG_ID)).thenReturn(true);
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(product("螺丝")));
        when(tagRepository.bindProduct(TAG_ID, PRODUCT_ID)).thenReturn(0);

        BusinessException ex =
                assertThrows(BusinessException.class, () -> service.bindTag(TAG_ID, PRODUCT_ID));

        assertEquals(ErrorCode.PARAM_ERROR, ex.getErrorCode());
    }

    @Test
    @DisplayName("绑定标签：成功后回读实体")
    void bindTagSuccess() {
        when(tagRepository.existsById(TAG_ID)).thenReturn(true);
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(product("螺丝")));
        when(tagRepository.bindProduct(TAG_ID, PRODUCT_ID)).thenReturn(1);
        when(tagRepository.findById(TAG_ID)).thenReturn(Optional.of(tag("BC1", null, null)));

        assertNotNull(service.bindTag(TAG_ID, PRODUCT_ID));

        verify(tagRepository).bindProduct(TAG_ID, PRODUCT_ID);
    }

    @Test
    @DisplayName("解绑标签：标签不存在抛 NOT_FOUND")
    void unbindTagMissing() {
        when(tagRepository.existsById(TAG_ID)).thenReturn(false);

        BusinessException ex =
                assertThrows(BusinessException.class, () -> service.unbindTag(TAG_ID));

        assertEquals(ErrorCode.NOT_FOUND, ex.getErrorCode());
    }

    @Test
    @DisplayName("解绑标签：更新行数为 0 时报 PARAM_ERROR（解绑失败）")
    void unbindTagNoRowUpdated() {
        when(tagRepository.existsById(TAG_ID)).thenReturn(true);
        when(tagRepository.unbindProduct(TAG_ID)).thenReturn(0);

        BusinessException ex =
                assertThrows(BusinessException.class, () -> service.unbindTag(TAG_ID));

        assertEquals(ErrorCode.PARAM_ERROR, ex.getErrorCode());
    }

    @Test
    @DisplayName("解绑标签：成功后回读实体")
    void unbindTagSuccess() {
        when(tagRepository.existsById(TAG_ID)).thenReturn(true);
        when(tagRepository.unbindProduct(TAG_ID)).thenReturn(1);
        when(tagRepository.findById(TAG_ID)).thenReturn(Optional.of(tag("BC1", null, null)));

        assertNotNull(service.unbindTag(TAG_ID));
    }

    // ---------------- 批量 ----------------

    @Test
    @DisplayName("批量绑定：产品ID为空被拒")
    void batchBindRejectsNullProductId() {
        ProductTagBatchBindRequest request = new ProductTagBatchBindRequest();
        request.setTagIds(List.of(TAG_ID));

        BusinessException ex =
                assertThrows(BusinessException.class, () -> service.batchBindTags(request));

        assertEquals(ErrorCode.PARAM_ERROR, ex.getErrorCode());
    }

    @Test
    @DisplayName("批量绑定：标签列表为空被拒")
    void batchBindRejectsEmptyTagIds() {
        ProductTagBatchBindRequest request = new ProductTagBatchBindRequest();
        request.setProductId(PRODUCT_ID);
        request.setTagIds(List.of());

        BusinessException ex =
                assertThrows(BusinessException.class, () -> service.batchBindTags(request));

        assertEquals(ErrorCode.PARAM_ERROR, ex.getErrorCode());
    }

    @Test
    @DisplayName("批量绑定：产品不存在抛 NOT_FOUND")
    void batchBindRejectsMissingProduct() {
        ProductTagBatchBindRequest request = new ProductTagBatchBindRequest();
        request.setProductId(PRODUCT_ID);
        request.setTagIds(List.of(TAG_ID));
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.empty());

        BusinessException ex =
                assertThrows(BusinessException.class, () -> service.batchBindTags(request));

        assertEquals(ErrorCode.NOT_FOUND, ex.getErrorCode());
    }

    @Test
    @DisplayName("批量绑定：成功/失败计数按更新行数计算")
    void batchBindCountsSuccessAndFailure() {
        ProductTagBatchBindRequest request = new ProductTagBatchBindRequest();
        request.setProductId(PRODUCT_ID);
        request.setTagIds(List.of(1L, 2L, 3L));
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(product("螺丝")));
        when(tagRepository.batchBindProducts(request.getTagIds(), PRODUCT_ID)).thenReturn(2);

        BatchBindResult result = service.batchBindTags(request);

        assertEquals(PRODUCT_ID, result.getProductId());
        assertEquals(3, result.getTotalCount());
        assertEquals(2, result.getSuccessCount());
        assertEquals(1, result.getFailedCount());
    }

    @Test
    @DisplayName("带验证码批量绑定：先校验验证码，再走批量绑定")
    void batchBindWithCaptchaEnforcesCaptchaFirst() {
        ProductTagBatchBindRequestWithCaptcha request = new ProductTagBatchBindRequestWithCaptcha();
        request.setCaptchaId("cid");
        request.setCaptchaCode("1234");
        request.setProductId(PRODUCT_ID);
        request.setTagIds(List.of(1L, 2L));
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(product("螺丝")));
        when(tagRepository.batchBindProducts(request.getTagIds(), PRODUCT_ID)).thenReturn(2);

        BatchBindResult result = service.batchBindTagsWithCaptcha(request);

        verify(captchaApplicationService).enforceCaptcha("cid", "1234");
        assertEquals(2, result.getSuccessCount());
    }

    @Test
    @DisplayName("带验证码批量绑定：验证码不通过时不得触达任何仓储（安全约束）")
    void batchBindWithCaptchaStopsBeforeRepository() {
        ProductTagBatchBindRequestWithCaptcha request = new ProductTagBatchBindRequestWithCaptcha();
        request.setCaptchaId("cid");
        request.setCaptchaCode("bad");
        request.setProductId(PRODUCT_ID);
        request.setTagIds(List.of(1L));
        doThrow(new BusinessException(ErrorCode.PARAM_ERROR, "验证码错误"))
                .when(captchaApplicationService)
                .enforceCaptcha("cid", "bad");

        BusinessException ex =
                assertThrows(
                        BusinessException.class, () -> service.batchBindTagsWithCaptcha(request));

        assertEquals(ErrorCode.PARAM_ERROR, ex.getErrorCode());
        verifyNoInteractions(tagRepository);
        verifyNoInteractions(productRepository);
    }

    @Test
    @DisplayName("批量解绑：标签列表为空被拒")
    void batchUnbindRejectsEmptyTagIds() {
        BusinessException ex =
                assertThrows(BusinessException.class, () -> service.batchUnbindTags(List.of()));

        assertEquals(ErrorCode.PARAM_ERROR, ex.getErrorCode());

        BusinessException ex2 =
                assertThrows(BusinessException.class, () -> service.batchUnbindTags(null));
        assertEquals(ErrorCode.PARAM_ERROR, ex2.getErrorCode());
    }

    @Test
    @DisplayName("批量解绑：成功/失败计数")
    void batchUnbindCountsSuccessAndFailure() {
        when(tagRepository.batchUnbindProducts(List.of(1L, 2L, 3L))).thenReturn(1);

        BatchUnbindResult result = service.batchUnbindTags(List.of(1L, 2L, 3L));

        assertEquals(3, result.getTotalCount());
        assertEquals(1, result.getSuccessCount());
        assertEquals(2, result.getFailedCount());
    }
}
