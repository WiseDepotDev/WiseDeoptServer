package com.huicang.wise.application.inspection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.huicang.wise.common.api.ErrorCode;
import com.huicang.wise.common.exception.BusinessException;
import com.huicang.wise.domain.inspection.InspectionDetail;
import com.huicang.wise.domain.inspection.InspectionResultSummary;
import com.huicang.wise.domain.inspection.InspectionTask;
import com.huicang.wise.domain.tag.ProductTag;
import com.huicang.wise.infrastructure.persistence.repository.inspection.InspectionDetailRepository;
import com.huicang.wise.infrastructure.persistence.repository.inspection.InspectionResultSummaryRepository;
import com.huicang.wise.infrastructure.persistence.repository.inspection.InspectionTaskRepository;
import com.huicang.wise.infrastructure.persistence.repository.tag.ProductTagRepository;
import com.huicang.wise.infrastructure.persistence.repository.user.UserRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * {@link InspectionApplicationService} 的**结果落库与补录**子切片测试（P2-11 应用层补测，收尾批）： {@code createResult} /
 * {@code manualRecord} / {@code getInspectionDifferences}。
 *
 * <p>要点：
 *
 * <ul>
 *   <li>{@code createResult} 的统计**完全来自入参**；决策 13 选项 B 之后，末尾的 {@code calculateTaskResult(task,
 *       false)} **不会**再用"总数−已扫"覆盖它们（这是本批要钉住的约束）；
 *   <li>{@code manualRecord} 的两道闸门（任务存在 → 任务必须已完成），以及"标签能匹配到 ⇒ normal， 否则 abnormal"的分支；
 *   <li>{@code getInspectionDifferences} 仍返回 **7 条硬编码演示数据**（决策 13 选项 C 有意保留），
 *       本测试把它逐条钉住，使"改成真实计算"一定会被发现。
 * </ul>
 *
 * @author WiseDepot
 * @version 1.0
 * @since 2026-09-26
 */
@ExtendWith(MockitoExtension.class)
class InspectionApplicationServiceResultTest {

    @Mock private InspectionTaskRepository inspectionTaskRepository;
    @Mock private InspectionResultSummaryRepository inspectionResultSummaryRepository;
    @Mock private InspectionDetailRepository inspectionDetailRepository;
    @Mock private ProductTagRepository productTagRepository;
    @Mock private UserRepository userRepository;

    @InjectMocks private InspectionApplicationService service;

    @BeforeEach
    void stubCommonSaves() {
        lenient()
                .when(inspectionTaskRepository.save(any(InspectionTask.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        lenient()
                .when(inspectionResultSummaryRepository.save(any(InspectionResultSummary.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        // 完成消息会遍历所有用户；返回空列表即可跳过（该方法整段包在 try/catch 里）
        lenient().when(userRepository.findAll()).thenReturn(List.of());
    }

    private static InspectionTask task(long id, short status) {
        InspectionTask t = new InspectionTask();
        t.setTaskId(id);
        t.setStatus(status);
        t.setPlanId(1L);
        t.setWarehouseId(1L);
        t.setDeviceId(1L);
        return t;
    }

    // ---------------------------------------------------------------- createResult

    @Test
    @DisplayName("createResult：任务不存在 → NOT_FOUND，且不落结果")
    void createResultWhenTaskMissingShouldThrowNotFound() {
        when(inspectionTaskRepository.findById(99L)).thenReturn(Optional.empty());

        BusinessException ex =
                assertThrows(
                        BusinessException.class, () -> service.createResult(99L, 10, 8, 2, 1, 0));

        assertEquals(ErrorCode.NOT_FOUND, ex.getErrorCode());
        verify(inspectionResultSummaryRepository, never()).save(any(InspectionResultSummary.class));
    }

    @Test
    @DisplayName("createResult：统计**完全取自入参**，不被'总数−已扫'覆盖（决策 13 选项 B 的约束）")
    void createResultShouldUseInboundCountsVerbatim() {
        when(inspectionTaskRepository.findById(1L)).thenReturn(Optional.of(task(1L, (short) 1)));

        // total=10、normal=7、abnormal=1 ⇒ inspected=8；missing=5、extra=3 同时非 0
        // （修复前 calculateTaskResult 会把它们改成 net=2 / 0，且两只不能并存）
        InspectionResultDTO dto = service.createResult(1L, 10, 7, 1, 5, 3);

        ArgumentCaptor<InspectionTask> taskCaptor = ArgumentCaptor.forClass(InspectionTask.class);
        verify(inspectionTaskRepository).save(taskCaptor.capture());
        InspectionTask saved = taskCaptor.getValue();
        assertEquals((short) 2, saved.getStatus(), "createResult 应把任务置为已完成");
        assertEquals(100, saved.getProgress());
        assertNotNull(saved.getEndTime());
        assertEquals(10, saved.getTotalItems());
        assertEquals(8, saved.getInspectedItems(), "inspected = normal + abnormal（源码里的 Approx 口径）");
        assertEquals(7, saved.getNormalItems());
        assertEquals(1, saved.getAbnormalItems());
        assertEquals(5, saved.getMissingItems(), "入参 missing 不应被净差覆盖");
        assertEquals(3, saved.getExtraItems(), "missing 与 extra 可以同时非 0");

        ArgumentCaptor<InspectionResultSummary> sumCaptor =
                ArgumentCaptor.forClass(InspectionResultSummary.class);
        verify(inspectionResultSummaryRepository).save(sumCaptor.capture());
        InspectionResultSummary sum = sumCaptor.getValue();
        assertEquals(1L, sum.getTaskId());
        assertEquals(10, sum.getTotalExpected());
        assertEquals(8, sum.getTotalScanned());
        assertEquals(7, sum.getMatchedCount());
        assertEquals(5, sum.getMissingCount());
        assertEquals(3, sum.getExtraCount());

        assertNotNull(dto, "应返回由 summary 转换出的结果 DTO");
    }

    @Test
    @DisplayName("createResult：完成消息的收件人来自 userRepository.findAll（空列表时不发消息也不报错）")
    void createResultShouldNotFailWhenNoRecipients() {
        when(inspectionTaskRepository.findById(2L)).thenReturn(Optional.of(task(2L, (short) 1)));

        service.createResult(2L, 4, 4, 0, 0, 0);

        verify(userRepository).findAll();
    }

    @Test
    @DisplayName("createResult：同任务再次落结果 ⇒ 复用已有汇总行（不再撞 uk_task_id 抛重复键）")
    void createResultShouldReuseExistingSummary() {
        when(inspectionTaskRepository.findById(3L)).thenReturn(Optional.of(task(3L, (short) 1)));

        InspectionResultSummary existing = new InspectionResultSummary();
        existing.setResultId(7483L);
        existing.setTaskId(3L);
        existing.setCreateTime(LocalDateTime.of(2026, 10, 2, 21, 43));
        when(inspectionResultSummaryRepository.findByTaskId(3L)).thenReturn(List.of(existing));

        service.createResult(3L, 10, 8, 2, 1, 0);

        ArgumentCaptor<InspectionResultSummary> captor =
                ArgumentCaptor.forClass(InspectionResultSummary.class);
        verify(inspectionResultSummaryRepository).save(captor.capture());
        assertEquals(7483L, captor.getValue().getResultId(), "应更新既有汇总行（保留主键），而不是新建一行");
        assertEquals(
                LocalDateTime.of(2026, 10, 2, 21, 43),
                captor.getValue().getCreateTime(),
                "重复落结果不应改写创建时间");
    }

    // ---------------------------------------------------------------- manualRecord

    @Test
    @DisplayName("manualRecord：任务不存在 → NOT_FOUND")
    void manualRecordWhenTaskMissingShouldThrowNotFound() {
        ManualRecordRequest request = new ManualRecordRequest();
        request.setTaskId(99L);
        request.setItems(List.of());
        when(inspectionTaskRepository.findById(99L)).thenReturn(Optional.empty());

        BusinessException ex =
                assertThrows(BusinessException.class, () -> service.manualRecord(request));

        assertEquals(ErrorCode.NOT_FOUND, ex.getErrorCode());
    }

    @Test
    @DisplayName("manualRecord：**任务未完成时拒绝补录**（PARAM_ERROR）且不写明细")
    void manualRecordWhenTaskNotCompletedShouldThrowParamError() {
        ManualRecordRequest request = new ManualRecordRequest();
        request.setTaskId(1L);
        request.setItems(List.of());
        when(inspectionTaskRepository.findById(1L)).thenReturn(Optional.of(task(1L, (short) 1)));

        BusinessException ex =
                assertThrows(BusinessException.class, () -> service.manualRecord(request));

        assertEquals(ErrorCode.PARAM_ERROR, ex.getErrorCode());
        verify(inspectionDetailRepository, never()).save(any(InspectionDetail.class));
    }

    @Test
    @DisplayName("manualRecord：标签匹配到 ⇒ normal/matched=1；匹配不到 ⇒ abnormal/matched=0，并重算任务统计")
    void manualRecordShouldClassifyByTagLookupAndRecalculate() {
        when(inspectionTaskRepository.findById(3L)).thenReturn(Optional.of(task(3L, (short) 2)));

        ProductTag tag = new ProductTag();
        tag.setTagId(77L);
        tag.setProductId(5L);
        when(productTagRepository.findByRfid("RFID-OK")).thenReturn(Optional.of(tag));
        when(productTagRepository.findByRfid("RFID-UNKNOWN")).thenReturn(Optional.empty());

        ManualRecordRequest request = new ManualRecordRequest();
        request.setTaskId(3L);
        ManualRecordRequest.ManualRecordItem ok = new ManualRecordRequest.ManualRecordItem();
        ok.setRfid("RFID-OK");
        ok.setTid("TID-1");
        ok.setRemark("补录正常");
        ManualRecordRequest.ManualRecordItem unknown = new ManualRecordRequest.ManualRecordItem();
        unknown.setRfid("RFID-UNKNOWN");
        request.setItems(List.of(ok, unknown));

        // recalculateTaskResult 是**从仓储重新读明细**来重算的（不是读刚 save 的对象）⇒ 需模拟已落库的 2 条
        InspectionDetail persistedNormal = new InspectionDetail();
        persistedNormal.setMatched((short) 1);
        InspectionDetail persistedAbnormal = new InspectionDetail();
        persistedAbnormal.setMatched((short) 0);
        when(inspectionDetailRepository.findByTaskId(3L))
                .thenReturn(List.of(persistedNormal, persistedAbnormal));

        service.manualRecord(request);

        ArgumentCaptor<InspectionDetail> captor = ArgumentCaptor.forClass(InspectionDetail.class);
        verify(inspectionDetailRepository, times(2)).save(captor.capture());
        List<InspectionDetail> savedDetails = captor.getAllValues();
        assertEquals(3L, savedDetails.get(0).getTaskId());
        assertEquals("RFID-OK", savedDetails.get(0).getRfid());
        assertEquals(77L, savedDetails.get(0).getTagId(), "命中标签时应带上 tagId");
        assertEquals((short) 1, savedDetails.get(0).getMatched());
        assertEquals("normal", savedDetails.get(0).getStatus());
        assertEquals((short) 0, savedDetails.get(1).getMatched(), "未命中标签 ⇒ abnormal");
        assertEquals("abnormal", savedDetails.get(1).getStatus());
        assertNotNull(savedDetails.get(0).getScanTime());
        assertEquals("补录正常", savedDetails.get(0).getRemark());

        // 重算：明细 2 条、其中 matched=1 的 1 条 ⇒ inspected=2 / normal=1 / abnormal=1
        ArgumentCaptor<InspectionTask> taskCaptor = ArgumentCaptor.forClass(InspectionTask.class);
        verify(inspectionTaskRepository).save(taskCaptor.capture());
        InspectionTask recalculated = taskCaptor.getValue();
        assertEquals(2, recalculated.getInspectedItems());
        assertEquals(1, recalculated.getNormalItems());
        assertEquals(1, recalculated.getAbnormalItems());
        // missing/extra 仍来自那批**演示差异**（决策 13 选项 C 未修）
        assertEquals(1, recalculated.getMissingItems(), "演示差异里唯一一条 MISSING");
        assertEquals(0, recalculated.getExtraItems());
    }

    // ----------------------------------------------------------------
    // getInspectionDifferences（钉住演示数据）

    @Test
    @DisplayName("⚠现状固定：getInspectionDifferences 恒返回 7 条硬编码演示差异，且**忽略 taskId**")
    void getInspectionDifferencesPinsDemoData() {
        List<InspectionDifferenceVO> a = service.getInspectionDifferences(1L);
        List<InspectionDifferenceVO> b = service.getInspectionDifferences(999L);

        assertEquals(7, a.size(), "固定 7 条（源码里的演示数据）");
        assertEquals(7, b.size(), "换 taskId 结果不变 ⇒ 入参未被使用");
        assertEquals("无线AP", a.get(0).getProductName());
        assertEquals("螺丝", a.get(5).getProductName());
        assertEquals("MISSING", a.get(5).getStatus());
        assertEquals(
                1,
                a.stream().filter(d -> "MISSING".equals(d.getStatus())).count(),
                "只有 1 条 MISSING —— 这正是 missing=1 的来源");
        verify(inspectionTaskRepository, never()).findById(anyLong());
    }
}
