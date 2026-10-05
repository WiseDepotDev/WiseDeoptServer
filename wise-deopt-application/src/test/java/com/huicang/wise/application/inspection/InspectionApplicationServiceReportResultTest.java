package com.huicang.wise.application.inspection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.huicang.wise.application.dashboard.DashboardKpiCache;
import com.huicang.wise.domain.inspection.InspectionDetail;
import com.huicang.wise.domain.inspection.InspectionDifference;
import com.huicang.wise.domain.inspection.InspectionProgressEvent;
import com.huicang.wise.domain.inspection.InspectionProgressPublisher;
import com.huicang.wise.domain.inspection.InspectionResultSummary;
import com.huicang.wise.domain.inspection.InspectionTask;
import com.huicang.wise.infrastructure.persistence.repository.inspection.InspectionDetailRepository;
import com.huicang.wise.infrastructure.persistence.repository.inspection.InspectionDifferenceRepository;
import com.huicang.wise.infrastructure.persistence.repository.inspection.InspectionResultSummaryRepository;
import com.huicang.wise.infrastructure.persistence.repository.inspection.InspectionTaskRepository;
import com.huicang.wise.infrastructure.redis.RedisCacheManager;
import com.huicang.wise.infrastructure.redis.RedisCacheUtils;
import com.huicang.wise.infrastructure.redis.RedisKeys;
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
 * {@link InspectionApplicationService#reportResult} 单元测试（P2-11 应用层补测，P0 单块最大路径，约 200 行）。
 *
 * <p><b>本测试的定位是"把当前行为（含已知缺陷）逐条钉死"</b>，不是"证明当前行为正确"。 巡检结果链路上存在一组**假数据实现**（见《基线记录.md》§141
 * 的缺陷清单），若不用测试固定， 任何人都可能在不被察觉的情况下改动对外的数值语义。因此下面每一条"现状固定"断言都带专门的说明， 并且**一旦这些缺陷被修复，这些断言会失败** ——
 * 这正是它们的作用：迫使修复被显式确认。
 *
 * <p>覆盖的判定分支：
 *
 * <ul>
 *   <li>taskId 非数字 / 任务不存在 ⇒ 不触碰任务，也不重算差异；
 *   <li>进度 <100 且任务为 PENDING ⇒ 状态置 IN_PROGRESS；进度 ≥100 ⇒ COMPLETED 并写 endTime；
 *   <li>total_expected=0 ⇒ 直接 COMPLETED；
 *   <li>明细映射：normal ⇒ matched=1，其余 ⇒ 0；epc 为 null ⇒ 空串；timestamp 为空 ⇒ 取当前时间；
 *   <li>差异：先按 taskId 删除、再写入（顺序断言）；
 *   <li>Redis 缓存清理与进度事件只在任务存在 / taskId 非空时发生。
 * </ul>
 *
 * @author WiseDepot
 * @version 1.0
 * @since 2026-09-26
 */
@ExtendWith(MockitoExtension.class)
class InspectionApplicationServiceReportResultTest {

    @Mock private InspectionTaskRepository inspectionTaskRepository;
    @Mock private InspectionDetailRepository inspectionDetailRepository;
    @Mock private InspectionDifferenceRepository inspectionDifferenceRepository;
    @Mock private InspectionResultSummaryRepository inspectionResultSummaryRepository;
    @Mock private DashboardKpiCache dashboardKpiCache;
    @Mock private RedisCacheManager cacheManager;
    @Mock private InspectionProgressPublisher progressPublisher;

    @InjectMocks private InspectionApplicationService service;

    @BeforeEach
    void bindRedisFacade() {
        new RedisCacheUtils(cacheManager);
    }

    /**
     * `reportResult` 末尾必然落一条 {@link InspectionResultSummary}，并把它的返回值交给 `convertToResultDTO`；不桩就会拿到
     * null 并在**生产代码**里 NPE（实测踩到）。 这里统一让它回显入参，免得每个用例重复桩。用 `lenient()` 是因为个别用例走不到该分支时， 不应被"多余桩"判定为失败。
     */
    @org.junit.jupiter.api.BeforeEach
    void stubSummarySave() {
        org.mockito.Mockito.lenient()
                .when(inspectionResultSummaryRepository.save(any(InspectionResultSummary.class)))
                .thenAnswer(inv -> inv.getArgument(0));
    }

    private static InspectionTask task(long id, short status) {
        InspectionTask t = new InspectionTask();
        t.setTaskId(id);
        t.setStatus(status);
        return t;
    }

    private static InspectionReportItem item(String epc, String status, Long timestamp) {
        InspectionReportItem i = new InspectionReportItem();
        i.setEpc(epc);
        i.setStatus(status);
        i.setTimestamp(timestamp);
        return i;
    }

    private static InspectionReportRequest request(
            String taskId, Integer expected, Integer scanned) {
        InspectionReportRequest r = new InspectionReportRequest();
        r.setTaskId(taskId);
        r.setTotal_expected(expected);
        r.setTotal_scanned(scanned);
        return r;
    }

    // ---------------------------------------------------------------- 不触碰任务的路径

    @Test
    @DisplayName("taskId 非数字：不查任务、不重算差异、不发进度事件，提交仍成功（汇总取请求值）")
    void reportResultWithNonNumericTaskIdSkipsTaskPath() {
        InspectionReportRequest request = request("not-a-number", 5, 3);

        service.reportResult(request);

        // 不能断言 findById 完全不被调用 —— convertToResultDTO 会用 summary.taskId(=0) 再查一次。
        // 这里固定的是"任务路径的副作用一个都没发生"。
        verify(inspectionDifferenceRepository, never()).deleteByTaskId(anyLong());
        verify(progressPublisher, never()).publishProgress(any(InspectionProgressEvent.class));
        verify(cacheManager, never()).delete(anyString());

        ArgumentCaptor<InspectionResultSummary> captor =
                ArgumentCaptor.forClass(InspectionResultSummary.class);
        verify(inspectionResultSummaryRepository).save(captor.capture());
        assertEquals(0L, captor.getValue().getTaskId(), "无法解析 taskId 时落库的 taskId 记为 0");
        assertEquals(5, captor.getValue().getTotalExpected(), "无任务时汇总取请求里的 total_expected");
        assertEquals(3, captor.getValue().getTotalScanned());
    }

    @Test
    @DisplayName("taskId 合法但任务不存在：不重算差异、汇总退回请求值，但**仍会发出一条进度事件**（现状固定）")
    void reportResultWhenTaskMissingFallsBackToRequestValues() {
        when(inspectionTaskRepository.findById(7L)).thenReturn(Optional.empty());
        InspectionReportRequest request = request("7", 9, 4);

        service.reportResult(request);

        verify(inspectionDifferenceRepository, never()).deleteByTaskId(anyLong());
        // 现状：进度事件的条件是 `taskId != null`，**不要求任务真的存在** ⇒ 任务不存在时也会发一条，
        // 其 progress/status 取自"退回请求值"的汇总（因此 status/totalItems 可能为 null）。已登记于 §141。
        verify(progressPublisher).publishProgress(any(InspectionProgressEvent.class));
        ArgumentCaptor<InspectionResultSummary> captor =
                ArgumentCaptor.forClass(InspectionResultSummary.class);
        verify(inspectionResultSummaryRepository).save(captor.capture());
        assertEquals(7L, captor.getValue().getTaskId());
        assertEquals(9, captor.getValue().getTotalExpected());
    }

    // ---------------------------------------------------------------- 重复上报（幂等）

    @Test
    @DisplayName("重复上报（该任务已有汇总行）⇒ 幂等更新同一行，任务照常推进到 COMPLETED")
    void reportResultShouldReuseExistingSummaryOnRepeatReport() {
        InspectionTask task = task(9002L, (short) 1);
        when(inspectionTaskRepository.findById(9002L)).thenReturn(Optional.of(task));
        when(inspectionTaskRepository.save(any(InspectionTask.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        when(inspectionDifferenceRepository.save(any(InspectionDifference.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        InspectionResultSummary existing = new InspectionResultSummary();
        existing.setResultId(7483L);
        existing.setTaskId(9002L);
        existing.setCreateTime(LocalDateTime.of(2026, 10, 2, 21, 43));
        when(inspectionResultSummaryRepository.findByTaskId(9002L)).thenReturn(List.of(existing));

        // 设备端重复上报同一条任务（2026-10-02 实测：任务 9002 每轮都被重新下发并重新上报）
        service.reportResult(request("9002", 10, 10));

        ArgumentCaptor<InspectionResultSummary> captor =
                ArgumentCaptor.forClass(InspectionResultSummary.class);
        verify(inspectionResultSummaryRepository).save(captor.capture());
        assertEquals(
                7483L,
                captor.getValue().getResultId(),
                "应更新既有汇总行（保留 resultId），而不是再 insert 一行去撞 uk_task_id");
        assertEquals(
                LocalDateTime.of(2026, 10, 2, 21, 43),
                captor.getValue().getCreateTime(),
                "重复上报不应改写创建时间");
        // 修复前：这里会抛 DataIntegrityViolationException，@Transactional 把整笔上报回滚 ⇒
        // 任务状态永远停在 IN_PROGRESS，下发查询又把它当成待执行任务发回设备 —— 死循环。
        assertEquals((short) 2, task.getStatus(), "重复上报不再回滚，任务应正常推进到 COMPLETED");
        assertEquals(100, task.getProgress());
    }

    // ---------------------------------------------------------------- 进度与状态

    @Test
    @DisplayName("进度<100 时保留真实进度、状态置 IN_PROGRESS、不写 endTime（决策 13 选项 B 修复后的行为）")
    void reportResultShouldKeepComputedProgressWhenPartiallyScanned() {
        InspectionTask task = task(1L, (short) 0); // PENDING
        when(inspectionTaskRepository.findById(1L)).thenReturn(Optional.of(task));
        when(inspectionTaskRepository.save(any(InspectionTask.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        when(inspectionDifferenceRepository.save(any(InspectionDifference.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        // 10 件里只扫了 5 件 ⇒ 进度 50
        service.reportResult(request("1", 10, 5));

        // 修复前：calculateTaskResult 无条件 setProgress(100)，于是出现"进度 100 但状态仍进行中、无结束时间"。
        // 修复后：进度保持按扫描比例算出的 50，状态由 PENDING 转 IN_PROGRESS，且**不写 endTime** —— 三者自洽。
        assertEquals(50, task.getProgress(), "进度应反映真实扫描比例，不能被强制写成 100");
        assertEquals((short) 1, task.getStatus(), "未扫满 ⇒ IN_PROGRESS");
        assertNull(task.getEndTime(), "未完成就不应有结束时间");
    }

    @Test
    @DisplayName("扫满（scanned≥expected）时状态置 COMPLETED 并写 endTime")
    void reportResultShouldCompleteTaskWhenFullyScanned() {
        InspectionTask task = task(2L, (short) 1); // IN_PROGRESS
        when(inspectionTaskRepository.findById(2L)).thenReturn(Optional.of(task));
        when(inspectionTaskRepository.save(any(InspectionTask.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        when(inspectionDifferenceRepository.save(any(InspectionDifference.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        service.reportResult(request("2", 10, 10));

        assertEquals((short) 2, task.getStatus());
        assertNotNull(task.getEndTime(), "完成时应写入结束时间");
        assertEquals(100, task.getProgress());
    }

    @Test
    @DisplayName("total_expected=0 时直接判定完成（写了 endTime）")
    void reportResultWithZeroExpectedShouldCompleteImmediately() {
        InspectionTask task = task(3L, (short) 0);
        when(inspectionTaskRepository.findById(3L)).thenReturn(Optional.of(task));
        when(inspectionTaskRepository.save(any(InspectionTask.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        when(inspectionDifferenceRepository.save(any(InspectionDifference.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        service.reportResult(request("3", 0, 0));

        assertEquals((short) 2, task.getStatus());
        assertEquals(100, task.getProgress());
        assertNotNull(task.getEndTime());
    }

    @Test
    @DisplayName("missing/extra 按**差异分类**统计而非净差（决策 13 选项 B 修复后的行为）—— 值本身仍是演示数据")
    void reportResultShouldDeriveMissingExtraFromDifferencesNotNetDifference() {
        InspectionTask task = task(4L, (short) 1);
        when(inspectionTaskRepository.findById(4L)).thenReturn(Optional.of(task));
        when(inspectionTaskRepository.save(any(InspectionTask.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        when(inspectionDifferenceRepository.save(any(InspectionDifference.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        // 期望 10、扫描 6 ⇒ 净差是 4，但**修复后不再用净差**：改用差异分类求和。
        service.reportResult(request("4", 10, 6));

        // 修复前：missing = 10-6 = 4、extra = 0（净差，且 extra 永远为 0）。
        // 修复后：missing = 差异里唯一一条 MISSING（螺丝：期望 1 / 扫描 0）= 1；没有 EXTRA 行 ⇒ extra = 0。
        // ⚠ 注意：这个 1 仍然来自 getInspectionDifferences 的**硬编码演示数据**（F1/F2 属决策 13 选项 A/B，
        // 本批未实施）—— 修好的是"统计口径"，不是"差异来源"。
        assertEquals(1, task.getMissingItems(), "按差异分类求和（当前差异来源仍是演示数据）");
        assertEquals(0, task.getExtraItems());
    }

    // ---------------------------------------------------------------- 明细映射

    @Test
    @DisplayName("明细映射：normal ⇒ matched=1、其余 ⇒ 0；epc 为 null ⇒ 空串；无 timestamp ⇒ 取当前时间")
    void reportResultShouldMapDetails() {
        InspectionTask task = task(5L, (short) 1);
        when(inspectionTaskRepository.findById(5L)).thenReturn(Optional.of(task));
        when(inspectionTaskRepository.save(any(InspectionTask.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        when(inspectionDifferenceRepository.save(any(InspectionDifference.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        InspectionReportRequest request = request("5", 10, 2);
        request.setDetails(
                List.of(item("EPC-1", "normal", 1_700_000_000L), item(null, "loss", null)));

        service.reportResult(request);

        ArgumentCaptor<InspectionDetail> captor = ArgumentCaptor.forClass(InspectionDetail.class);
        verify(inspectionDetailRepository, times(2)).save(captor.capture());
        List<InspectionDetail> saved = captor.getAllValues();

        assertEquals("EPC-1", saved.get(0).getRfid());
        assertEquals((short) 1, saved.get(0).getMatched(), "status=normal ⇒ matched=1");
        assertNotNull(saved.get(0).getScanTime(), "给了 timestamp 时按该时刻换算扫描时间");

        assertEquals("", saved.get(1).getRfid(), "epc 为 null 时应落空串，而不是 null");
        assertEquals((short) 0, saved.get(1).getMatched(), "非 normal ⇒ matched=0");
        assertNotNull(saved.get(1).getScanTime(), "未给 timestamp 时回退到当前时间");
    }

    // ---------------------------------------------------------------- 差异重算（含缺陷固定）

    @Test
    @DisplayName("⚠现状固定：差异是 7 条**硬编码假数据**，先删后写，并覆盖任务统计（服务端并未按库存计算）")
    void reportResultPinsFakeDifferencesDefect() {
        InspectionTask task = task(6L, (short) 1);
        when(inspectionTaskRepository.findById(6L)).thenReturn(Optional.of(task));
        when(inspectionTaskRepository.save(any(InspectionTask.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        when(inspectionDifferenceRepository.save(any(InspectionDifference.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        service.reportResult(request("6", 10, 4));

        // 顺序：先按 taskId 清掉旧差异，再逐条写入
        inOrder(inspectionDifferenceRepository)
                .verify(inspectionDifferenceRepository)
                .deleteByTaskId(6L);
        ArgumentCaptor<InspectionDifference> captor =
                ArgumentCaptor.forClass(InspectionDifference.class);
        verify(inspectionDifferenceRepository, times(7)).save(captor.capture());

        // 这 7 条是源码里写死的（"无线AP/路由器/显示器/小型服务器/螺丝/扫描器"），与本任务的真实库存无关
        List<InspectionDifference> saved = captor.getAllValues();
        assertEquals(6L, saved.get(0).getTaskId());
        assertEquals("无线AP", saved.get(0).getProductName());
        assertEquals("螺丝", saved.get(5).getProductName());
        assertEquals("MISSING", saved.get(5).getStatus());
        assertEquals(
                1,
                saved.stream().filter(d -> "MISSING".equals(d.getStatus())).count(),
                "假数据里只有 1 条 MISSING；这正是后面 missing=1 的来源（随后又被净差覆盖）");
    }

    @Test
    @DisplayName("任务存在时清 Redis 缓存键 inspection:task:<id>，并发出一次进度事件")
    void reportResultShouldEvictCacheAndPublishProgress() {
        InspectionTask task = task(8L, (short) 1);
        when(inspectionTaskRepository.findById(8L)).thenReturn(Optional.of(task));
        when(inspectionTaskRepository.save(any(InspectionTask.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        when(inspectionDifferenceRepository.save(any(InspectionDifference.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        service.reportResult(request("8", 10, 10));

        verify(cacheManager).delete(RedisKeys.inspectionTask(8L));
        ArgumentCaptor<InspectionProgressEvent> captor =
                ArgumentCaptor.forClass(InspectionProgressEvent.class);
        verify(progressPublisher).publishProgress(captor.capture());
        assertEquals(8L, captor.getValue().getTaskId());
    }

    // ---------------------------------------------------------------- 结果查询（含缺陷固定）

    @Test
    @DisplayName("⚠现状固定：getResult 返回**完全假数据**（固定 7/6/6/1/0、COMPLETED、taskId=1），不查库")
    void getResultPinsFakeResultDefect() {
        InspectionResultDTO dto = service.getResult(42L);

        assertEquals(42L, dto.getResultId(), "resultId 回显入参");
        assertEquals(1L, dto.getTaskId(), "taskId 恒为 1（假数据）");
        assertEquals(7, dto.getTotalItems());
        assertEquals(6, dto.getTotalScanned());
        assertEquals(1, dto.getMissingItems());
        assertEquals("COMPLETED", dto.getStatus());
        verify(inspectionResultSummaryRepository, never()).findById(anyLong());
    }

    @Test
    @DisplayName("⚠现状固定：listResults **忽略全部筛选参数**，恒返回 1 条假结果")
    void listResultsPinsFakeListDefect() {
        List<InspectionResultDTO> a = service.listResults(1L, 1L, "COMPLETED");
        List<InspectionResultDTO> b = service.listResults(999L, 888L, "PENDING");

        assertEquals(1, a.size());
        assertEquals(1, b.size(), "换了筛选条件结果集不变 ⇒ 参数未被使用");
        assertEquals(1L, a.get(0).getResultId());
        assertEquals(1L, b.get(0).getResultId());
        verify(inspectionResultSummaryRepository, never()).findByTaskId(anyLong());
    }
}
