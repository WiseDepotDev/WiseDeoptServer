package com.huicang.wise.application.warehouse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.huicang.wise.common.api.ErrorCode;
import com.huicang.wise.common.exception.BusinessException;
import com.huicang.wise.domain.repository.warehouse.WarehouseRepository;
import com.huicang.wise.domain.warehouse.Warehouse;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * {@link WarehouseApplicationService} 单元测试（P2-11）。
 *
 * <p>覆盖 P2-06 引入的「不存在即抛 {@link BusinessException}(NOT_FOUND)」语义： 此前该处抛的是裸 {@link
 * RuntimeException}，会被全局兜底映射为 500， 现在必须是 404 语义的业务异常。
 *
 * @author WiseDepot
 * @version 1.0
 * @since 2026-02-27
 */
class WarehouseApplicationServiceTest {

    private WarehouseRepository repository;

    private WarehouseApplicationService service;

    @BeforeEach
    void setUp() {
        repository = mock(WarehouseRepository.class);
        service = new WarehouseApplicationService(repository);
    }

    /**
     * 构造仓库实体。
     *
     * @param id 主键
     * @param name 名称
     * @return 实体
     */
    private Warehouse warehouse(Long id, String name) {
        Warehouse entity = new Warehouse();
        entity.setWarehouseId(id);
        entity.setWarehouseName(name);
        entity.setWarehouseCode("WH-" + id);
        return entity;
    }

    /** 按关键字查询应返回 DTO 列表。 */
    @Test
    @DisplayName("shouldReturnDtoListWhenListWarehouses")
    void shouldReturnDtoListWhenListWarehouses() {
        when(repository.findByKeyword(any())).thenReturn(List.of(warehouse(1L, "一号库")));

        List<WarehouseDTO> result = service.listWarehouses("一号");

        assertEquals(1, result.size());
        assertEquals("一号库", result.get(0).getWarehouseName());
    }

    /** 关键字（含 null）应原样透传给仓储，由仓储决定匹配策略。 */
    @Test
    @DisplayName("shouldPassKeywordToRepositoryIncludingNull")
    void shouldPassKeywordToRepositoryIncludingNull() {
        when(repository.findByKeyword(any())).thenReturn(Collections.emptyList());

        assertTrue(service.listWarehouses(null).isEmpty());
        assertTrue(service.listWarehouses("  ").isEmpty());
        verify(repository).findByKeyword(null);
        verify(repository).findByKeyword("  ");
    }

    /** 查询不存在的仓库必须抛 BusinessException(NOT_FOUND)，而不是 RuntimeException。 */
    @Test
    @DisplayName("shouldThrowNotFoundBusinessExceptionWhenGetMissingWarehouse")
    void shouldThrowNotFoundBusinessExceptionWhenGetMissingWarehouse() {
        when(repository.findById(anyLong())).thenReturn(Optional.empty());

        BusinessException ex =
                assertThrows(BusinessException.class, () -> service.getWarehouse(404L));

        assertEquals(ErrorCode.NOT_FOUND, ex.getErrorCode());
        assertTrue(ex.getMessage().contains("404"), "异常信息应包含缺失的主键: " + ex.getMessage());
    }

    /** 更新不存在的仓库同样应为 NOT_FOUND。 */
    @Test
    @DisplayName("shouldThrowNotFoundWhenUpdateMissingWarehouse")
    void shouldThrowNotFoundWhenUpdateMissingWarehouse() {
        when(repository.findById(anyLong())).thenReturn(Optional.empty());

        WarehouseUpdateRequest request = new WarehouseUpdateRequest();
        request.setWarehouseName("新名称");

        BusinessException ex =
                assertThrows(BusinessException.class, () -> service.updateWarehouse(9L, request));

        assertEquals(ErrorCode.NOT_FOUND, ex.getErrorCode());
    }

    /** 删除不存在的仓库不得调用仓储删除。 */
    @Test
    @DisplayName("shouldNotDeleteWhenWarehouseMissing")
    void shouldNotDeleteWhenWarehouseMissing() {
        when(repository.existsById(anyLong())).thenReturn(false);

        assertThrows(BusinessException.class, () -> service.deleteWarehouse(7L));
        verify(repository, never()).deleteById(anyLong());
    }

    /** 正常创建应返回 DTO 并落库。 */
    @Test
    @DisplayName("shouldReturnDtoAndPersistWhenCreateWarehouse")
    void shouldReturnDtoAndPersistWhenCreateWarehouse() {
        when(repository.save(any(Warehouse.class)))
                .thenAnswer(
                        invocation -> {
                            Warehouse saved = invocation.getArgument(0);
                            saved.setWarehouseId(100L);
                            return saved;
                        });

        WarehouseCreateRequest request = new WarehouseCreateRequest();
        request.setWarehouseName("二号库");
        request.setWarehouseCode("WH-100");

        WarehouseDTO dto = service.createWarehouse(request);

        assertNotNull(dto.getWarehouseId());
        assertEquals("二号库", dto.getWarehouseName());
        verify(repository).save(any(Warehouse.class));
    }
}
