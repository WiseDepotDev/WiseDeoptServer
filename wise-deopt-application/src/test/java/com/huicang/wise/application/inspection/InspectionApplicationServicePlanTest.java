package com.huicang.wise.application.inspection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.huicang.wise.common.api.ErrorCode;
import com.huicang.wise.common.exception.BusinessException;
import com.huicang.wise.domain.inspection.InspectionPlan;
import com.huicang.wise.infrastructure.persistence.repository.inspection.InspectionPlanRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * {@link InspectionApplicationService} 的**巡检计划（Plan）子切片**单元测试（P2-11 应用层补测）。
 *
 * <p>为什么先切这一块：`InspectionApplicationService` 有 **950 行 / 13 个公开方法 / 16 个依赖**， 是全仓未覆盖行最多的类（531 行）。其中
 * Plan 相关方法（create/update/delete/get/list） 只依赖 1 个仓储，是**能独立钉死**的一块；`reportResult`（约 200
 * 行）依赖面大得多，留作下一批。
 *
 * <p>本测试有两条"把现状固定住"的断言，目的不是证明它对，而是**让行为变化一定会被发现**：
 *
 * <ol>
 *   <li>{@code updatePlan} 只覆盖**非 null** 字段 —— 传 null 表示"不改这一项"，不是"清空"；
 *   <li>{@code listPlans} 的 {@code warehouseId} 参数**当前被忽略**（只用 status 查询）——
 *       若将来实现了按仓库过滤，这条断言会失败，从而迫使改动被显式确认。
 * </ol>
 *
 * @author WiseDepot
 * @version 1.0
 * @since 2026-09-26
 */
@ExtendWith(MockitoExtension.class)
class InspectionApplicationServicePlanTest {

    @Mock private InspectionPlanRepository inspectionPlanRepository;

    @InjectMocks private InspectionApplicationService service;

    private static InspectionPlan plan(Long id, String name, Short status) {
        InspectionPlan p = new InspectionPlan();
        p.setPlanId(id);
        p.setPlanName(name);
        p.setDeviceId(5L);
        p.setCronExpression("0 0 2 * * ?");
        p.setStatus(status);
        p.setCreateTime(LocalDateTime.of(2026, 9, 1, 8, 0));
        p.setUpdateTime(LocalDateTime.of(2026, 9, 2, 9, 0));
        return p;
    }

    private static InspectionPlanCreateRequest createRequest(String name) {
        InspectionPlanCreateRequest r = new InspectionPlanCreateRequest();
        r.setPlanName(name);
        r.setDeviceId(5L);
        r.setCronExpression("0 0 2 * * ?");
        return r;
    }

    // ---------------------------------------------------------------- createPlan

    @Test
    @DisplayName("createPlan：同名计划已存在 → PARAM_ERROR，且不写库")
    void createPlanWhenNameExistsShouldThrowParamError() {
        when(inspectionPlanRepository.findByPlanName("每日巡检"))
                .thenReturn(Optional.of(plan(1L, "每日巡检", (short) 1)));

        BusinessException ex =
                assertThrows(
                        BusinessException.class, () -> service.createPlan(createRequest("每日巡检")));

        assertEquals(ErrorCode.PARAM_ERROR, ex.getErrorCode());
        verify(inspectionPlanRepository, never()).save(any(InspectionPlan.class));
    }

    @Test
    @DisplayName("createPlan：成功时 status=1、createBy/updateBy=1、时间戳齐全，并按 status 推出 enabled=true")
    void createPlanShouldStampDefaultsAndReturnEnabled() {
        when(inspectionPlanRepository.findByPlanName("新计划")).thenReturn(Optional.empty());
        when(inspectionPlanRepository.save(any(InspectionPlan.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        InspectionPlanDTO dto = service.createPlan(createRequest("新计划"));

        ArgumentCaptor<InspectionPlan> captor = ArgumentCaptor.forClass(InspectionPlan.class);
        verify(inspectionPlanRepository).save(captor.capture());
        InspectionPlan saved = captor.getValue();
        assertEquals("新计划", saved.getPlanName());
        assertEquals(5L, saved.getDeviceId());
        assertEquals("0 0 2 * * ?", saved.getCronExpression());
        assertEquals((short) 1, saved.getStatus(), "新建计划默认启用（status=1）");
        assertEquals(1L, saved.getCreateBy());
        assertEquals(1L, saved.getUpdateBy());
        assertNotNull(saved.getCreateTime());
        assertNotNull(saved.getUpdateTime());
        assertTrue(dto.getEnabled(), "status=1 应映射为 enabled=true");
        assertEquals("新计划", dto.getPlanName());
    }

    // ---------------------------------------------------------------- updatePlan

    @Test
    @DisplayName("updatePlan：计划不存在 → NOT_FOUND，且不保存")
    void updatePlanWhenMissingShouldThrowNotFound() {
        InspectionPlanUpdateRequest request = new InspectionPlanUpdateRequest();
        request.setPlanName("x");
        when(inspectionPlanRepository.findById(99L)).thenReturn(Optional.empty());

        BusinessException ex =
                assertThrows(BusinessException.class, () -> service.updatePlan(99L, request));

        assertEquals(ErrorCode.NOT_FOUND, ex.getErrorCode());
        verify(inspectionPlanRepository, never()).save(any(InspectionPlan.class));
    }

    @Test
    @DisplayName("updatePlan：**只覆盖非 null 字段** —— 传 null 表示'不改'，不是'清空'（现状固定）")
    void updatePlanShouldApplyOnlyNonNullFields() {
        InspectionPlan existing = plan(3L, "原名", (short) 1);
        when(inspectionPlanRepository.findById(3L)).thenReturn(Optional.of(existing));
        when(inspectionPlanRepository.save(any(InspectionPlan.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        InspectionPlanUpdateRequest request = new InspectionPlanUpdateRequest();
        request.setCronExpression("0 30 3 * * ?"); // 只改 cron，其余留 null
        service.updatePlan(3L, request);

        assertEquals("原名", existing.getPlanName(), "planName 传 null 时不应被清空");
        assertEquals(5L, existing.getDeviceId(), "deviceId 传 null 时不应被清空");
        assertEquals("0 30 3 * * ?", existing.getCronExpression());
        assertEquals((short) 1, existing.getStatus(), "status 不在更新请求里，不应被改动");
        assertEquals(1L, existing.getUpdateBy());
        assertNotNull(existing.getUpdateTime());
    }

    @Test
    @DisplayName("updatePlan：非 null 字段全部生效")
    void updatePlanShouldApplyAllProvidedFields() {
        InspectionPlan existing = plan(3L, "原名", (short) 0);
        when(inspectionPlanRepository.findById(3L)).thenReturn(Optional.of(existing));
        when(inspectionPlanRepository.save(any(InspectionPlan.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        InspectionPlanUpdateRequest request = new InspectionPlanUpdateRequest();
        request.setPlanName("新名");
        request.setDeviceId(9L);
        request.setCronExpression("0 0 5 * * ?");
        service.updatePlan(3L, request);

        assertEquals("新名", existing.getPlanName());
        assertEquals(9L, existing.getDeviceId());
        assertEquals("0 0 5 * * ?", existing.getCronExpression());
    }

    // ---------------------------------------------------------------- deletePlan / getPlan

    @Test
    @DisplayName("deletePlan：不存在 → NOT_FOUND，且不调用 delete")
    void deletePlanWhenMissingShouldThrowNotFound() {
        when(inspectionPlanRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(BusinessException.class, () -> service.deletePlan(99L));

        verify(inspectionPlanRepository, never()).delete(any(InspectionPlan.class));
    }

    @Test
    @DisplayName("deletePlan：存在时删除的是**查出来的那个实体**")
    void deletePlanShouldDeleteLoadedEntity() {
        InspectionPlan existing = plan(4L, "待删", (short) 1);
        when(inspectionPlanRepository.findById(4L)).thenReturn(Optional.of(existing));

        service.deletePlan(4L);

        verify(inspectionPlanRepository).delete(existing);
    }

    @Test
    @DisplayName("getPlan：不存在 → NOT_FOUND")
    void getPlanWhenMissingShouldThrowNotFound() {
        when(inspectionPlanRepository.findById(99L)).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class, () -> service.getPlan(99L));

        assertEquals(ErrorCode.NOT_FOUND, ex.getErrorCode());
    }

    @Test
    @DisplayName("getPlan：status=1 → enabled=true；status=0 或 null → enabled=false")
    void getPlanShouldDeriveEnabledFromStatus() {
        when(inspectionPlanRepository.findById(1L))
                .thenReturn(Optional.of(plan(1L, "启用", (short) 1)));
        when(inspectionPlanRepository.findById(2L))
                .thenReturn(Optional.of(plan(2L, "停用", (short) 0)));
        when(inspectionPlanRepository.findById(3L)).thenReturn(Optional.of(plan(3L, "无状态", null)));

        assertTrue(service.getPlan(1L).getEnabled());
        assertFalse(service.getPlan(2L).getEnabled());
        assertFalse(service.getPlan(3L).getEnabled(), "status 为 null 时不能抛 NPE，应视为未启用");
    }

    // ---------------------------------------------------------------- listPlans

    @Test
    @DisplayName("listPlans：enabled=true → 按 status=1 查询；false → 0；null → 不按状态过滤")
    void listPlansShouldTranslateEnabledToStatus() {
        when(inspectionPlanRepository.findByConditions(any())).thenReturn(List.of());

        service.listPlans(null, true, null);
        verify(inspectionPlanRepository).findByConditions((short) 1);

        service.listPlans(null, false, null);
        verify(inspectionPlanRepository).findByConditions((short) 0);

        service.listPlans(null, null, null);
        verify(inspectionPlanRepository).findByConditions(null);
    }

    @Test
    @DisplayName("listPlans：planType 非法/为空时退化为 null，**不抛异常**")
    void listPlansShouldTolerateBadPlanType() {
        when(inspectionPlanRepository.findByConditions(any())).thenReturn(List.of());

        service.listPlans("not-a-number", null, null);
        service.listPlans("", null, null);
        service.listPlans(null, null, null);

        // 三次调用都应带 status=null（planType 解析失败不改变 status，也不抛出）
        verify(inspectionPlanRepository, org.mockito.Mockito.times(3)).findByConditions(null);
    }

    @Test
    @DisplayName("listPlans：**warehouseId 当前被忽略**（只用 status 查询）—— 把现状固定住")
    void listPlansCurrentlyIgnoresWarehouseId() {
        when(inspectionPlanRepository.findByConditions((short) 1)).thenReturn(List.of());

        service.listPlans("0", true, 12345L);

        // 断言"只按 status 查"：换成按仓库过滤后这条会失败，从而迫使改动被显式确认
        verify(inspectionPlanRepository).findByConditions((short) 1);
        verify(inspectionPlanRepository, never()).findByConditions(null);
    }

    @Test
    @DisplayName("listPlans：把每个计划都映射成 DTO，顺序保持")
    void listPlansShouldMapEveryPlan() {
        when(inspectionPlanRepository.findByConditions((short) 1))
                .thenReturn(List.of(plan(1L, "A", (short) 1), plan(2L, "B", (short) 1)));

        List<InspectionPlanDTO> result = service.listPlans("0", true, null);

        assertEquals(2, result.size());
        assertEquals("A", result.get(0).getPlanName());
        assertEquals("B", result.get(1).getPlanName());
        assertTrue(result.get(0).getEnabled());
    }
}
