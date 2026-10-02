package com.huicang.wise.application.inspection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.huicang.wise.common.api.ErrorCode;
import com.huicang.wise.common.exception.BusinessException;
import com.huicang.wise.domain.device.DeviceCore;
import com.huicang.wise.domain.inspection.InspectionProgressPublisher;
import com.huicang.wise.domain.inspection.InspectionTask;
import com.huicang.wise.domain.inspection.TaskPublisher;
import com.huicang.wise.infrastructure.persistence.repository.device.DeviceRepository;
import com.huicang.wise.infrastructure.persistence.repository.inspection.InspectionDetailRepository;
import com.huicang.wise.infrastructure.persistence.repository.inspection.InspectionPlanRepository;
import com.huicang.wise.infrastructure.persistence.repository.inspection.InspectionTaskRepository;
import com.huicang.wise.infrastructure.persistence.repository.warehouse.WarehouseRepository;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * {@link InspectionApplicationService} 的**任务生命周期**子切片测试（P2-11 应用层补测）： {@code createTask} / {@code
 * getTask} / {@code listTasks}（两个重载）/ {@code updateTaskStatus} / {@code updateTaskProgress}。
 *
 * <p>这一块的价值在于**闸门与状态机**：
 *
 * <ul>
 *   <li>{@code createTask} 的三道校验（设备存在 → 仓库 ID 非空 → 仓库存在）顺序与错误码；
 *   <li>{@code updateTaskStatus} 的 <b>COMPLETED 分支</b>：写 endTime、进度置 100， 并在 missing/extra
 *       **尚未计算过**时用"总数−已扫"兜底（决策 13 选项 B 修复后的语义）；
 *   <li>{@code updateTaskProgress} 的自动状态迁移（≥100 ⇒ COMPLETED 且补 endTime； 0&lt;progress&lt;100 且
 *       PENDING ⇒ IN_PROGRESS 且补 startTime），以及"清 dashboard KPI 缓存"。
 * </ul>
 *
 * @author WiseDepot
 * @version 1.0
 * @since 2026-09-26
 */
@ExtendWith(MockitoExtension.class)
class InspectionApplicationServiceTaskTest {

    @Mock private InspectionPlanRepository inspectionPlanRepository;
    @Mock private InspectionDetailRepository inspectionDetailRepository;
    @Mock private InspectionTaskRepository inspectionTaskRepository;
    @Mock private DeviceRepository deviceRepository;
    @Mock private WarehouseRepository warehouseRepository;
    @Mock private TaskPublisher taskPublisher;
    @Mock private StringRedisTemplate stringRedisTemplate;
    @Mock private InspectionProgressPublisher progressPublisher;

    @InjectMocks private InspectionApplicationService service;

    /** `convertToTaskDTO` 会用到任务类型/状态的中文名映射，不需要额外桩；此处只统一 save 回显。 */
    @BeforeEach
    void stubSave() {
        lenient()
                .when(inspectionTaskRepository.save(any(InspectionTask.class)))
                .thenAnswer(inv -> inv.getArgument(0));
    }

    private static InspectionTask task(long id, short status) {
        InspectionTask t = new InspectionTask();
        t.setTaskId(id);
        t.setStatus(status);
        t.setPlanId(1L);
        t.setWarehouseId(1L);
        t.setDeviceId(1L);
        t.setTaskType((short) 1);
        t.setCreateTime(LocalDateTime.of(2026, 9, 1, 8, 0));
        t.setUpdateTime(LocalDateTime.of(2026, 9, 1, 8, 0));
        return t;
    }

    private static InspectionTaskCreateRequest createRequest(Long deviceId, Long warehouseId) {
        InspectionTaskCreateRequest r = new InspectionTaskCreateRequest();
        r.setDeviceId(deviceId);
        r.setWarehouseId(warehouseId);
        r.setPlanId(1L);
        r.setTargetDistance(1000f);
        return r;
    }

    // ---------------------------------------------------------------- createTask

    @Test
    @DisplayName("createTask：设备不存在 → NOT_FOUND（且不校验仓库、不写库、不发 MQTT）")
    void createTaskWhenDeviceMissingShouldThrowNotFound() {
        when(deviceRepository.findById(9L)).thenReturn(Optional.empty());

        BusinessException ex =
                assertThrows(
                        BusinessException.class, () -> service.createTask(createRequest(9L, 1L)));

        assertEquals(ErrorCode.NOT_FOUND, ex.getErrorCode());
        verify(warehouseRepository, never()).existsById(anyLong());
        verify(inspectionTaskRepository, never()).save(any(InspectionTask.class));
        verify(taskPublisher, never()).publishTask(anyString(), any());
    }

    @Test
    @DisplayName("createTask：仓库 ID 为空 → PARAM_ERROR（校验顺序在设备之后）")
    void createTaskWhenWarehouseIdMissingShouldThrowParamError() {
        when(deviceRepository.findById(1L)).thenReturn(Optional.of(new DeviceCore()));

        BusinessException ex =
                assertThrows(
                        BusinessException.class, () -> service.createTask(createRequest(1L, null)));

        assertEquals(ErrorCode.PARAM_ERROR, ex.getErrorCode());
        verify(inspectionTaskRepository, never()).save(any(InspectionTask.class));
    }

    @Test
    @DisplayName("createTask：仓库不存在 → NOT_FOUND")
    void createTaskWhenWarehouseMissingShouldThrowNotFound() {
        when(deviceRepository.findById(1L)).thenReturn(Optional.of(new DeviceCore()));
        when(warehouseRepository.existsById(7L)).thenReturn(false);

        BusinessException ex =
                assertThrows(
                        BusinessException.class, () -> service.createTask(createRequest(1L, 7L)));

        assertEquals(ErrorCode.NOT_FOUND, ex.getErrorCode());
    }

    @Test
    @DisplayName("createTask：成功后 status=0（待执行）、taskType=1（手动），并向该设备下发任务消息")
    void createTaskShouldPersistPendingTaskAndPublishToDevice() {
        DeviceCore device = new DeviceCore();
        device.setDeviceCode("DEV-001");
        when(deviceRepository.findById(1L)).thenReturn(Optional.of(device));
        when(warehouseRepository.existsById(1L)).thenReturn(true);

        service.createTask(createRequest(1L, 1L));

        ArgumentCaptor<InspectionTask> captor = ArgumentCaptor.forClass(InspectionTask.class);
        verify(inspectionTaskRepository).save(captor.capture());
        InspectionTask saved = captor.getValue();
        assertEquals((short) 0, saved.getStatus(), "新建任务应为待执行");
        assertEquals((short) 1, saved.getTaskType(), "默认为手动任务");
        assertEquals(1L, saved.getWarehouseId());
        assertEquals(1000f, saved.getTargetDistance());
        assertNotNull(saved.getCreateTime());
        assertNotNull(saved.getUpdateTime());

        verify(taskPublisher).publishTask(org.mockito.ArgumentMatchers.eq("DEV-001"), any());
    }

    // ---------------------------------------------------------------- getTask / listTasks

    @Test
    @DisplayName("getTask：不存在 → NOT_FOUND；存在时返回映射后的 DTO（含中文类型/状态名）")
    void getTaskShouldMapOrThrow() {
        when(inspectionTaskRepository.findById(99L)).thenReturn(Optional.empty());
        BusinessException ex = assertThrows(BusinessException.class, () -> service.getTask(99L));
        assertEquals(ErrorCode.NOT_FOUND, ex.getErrorCode());

        when(inspectionTaskRepository.findById(1L)).thenReturn(Optional.of(task(1L, (short) 0)));
        InspectionTaskDTO dto = service.getTask(1L);
        assertEquals(1L, dto.getTaskId());
        assertEquals((short) 0, dto.getStatus());
        assertNotNull(dto.getTaskTypeDesc(), "DTO 应带上类型的中文名");
        assertNotNull(dto.getStatusDesc(), "DTO 应带上状态的中文名");
    }

    @Test
    @DisplayName("listTasks（列表版）：taskType/status 为空白 ⇒ 退化为 null，并原样传给仓储")
    void listTasksShouldTolerateEmptyFilters() {
        when(inspectionTaskRepository.findByConditions(any(), any(), any(), any(), any()))
                .thenReturn(List.of());

        service.listTasks(1L, "  ", "", 2L, 3L);
        verify(inspectionTaskRepository).findByConditions(1L, 2L, null, null, 3L);

        service.listTasks(null, "2", "1", null, null);
        verify(inspectionTaskRepository).findByConditions(null, null, (short) 2, (short) 1, null);
    }

    @Test
    @DisplayName("listTasks：taskType 符号名 ⇒ 翻成类型码；认不出 ⇒ PARAM_ERROR（与 status 同一套策略）")
    void listTasksParsesTaskTypeAndRejectsUnknown() {
        when(inspectionTaskRepository.findByConditions(any(), any(), any(), any(), any()))
                .thenReturn(List.of());

        service.listTasks(null, "manual", "2", null, null);
        verify(inspectionTaskRepository).findByConditions(null, null, (short) 1, (short) 2, null);

        service.listTasks(null, "SCHEDULED", null, null, null);
        verify(inspectionTaskRepository).findByConditions(null, null, (short) 0, null, null);

        // 修复前这里会静默返回"全部类型"的任务；现在报错（同一个"不把条件放宽"的策略）。
        BusinessException ex =
                assertThrows(
                        BusinessException.class,
                        () -> service.listTasks(null, "定时", null, null, null));
        assertEquals(ErrorCode.PARAM_ERROR, ex.getErrorCode());
    }

    @Test
    @DisplayName("listTasks：status=pending（设备端实际发的符号名）⇒ 只查 PENDING(0)，不再退化成\"不过滤\"")
    void listTasksWithDeviceSymbolicPendingFiltersByPending() {
        when(inspectionTaskRepository.findByConditions(any(), any(), any(), any(), any()))
                .thenReturn(List.of(task(9002L, (short) 0)));

        List<InspectionTaskDTO> tasks = service.listTasks(null, null, "pending", null, null);

        assertEquals(1, tasks.size());
        // 设备端 patrol_http.c 拼的就是 status=pending；这里钉住它**必须**被翻成状态码 0。
        // 修复前 Short.parseShort("pending") 抛异常后被吞掉、taskStatus 留 null ⇒ 查询条件消失，
        // 设备每次轮询都拿到全部任务（含已完成/执行中），于是反复执行同一条任务。
        verify(inspectionTaskRepository).findByConditions(null, null, null, (short) 0, null);
    }

    @Test
    @DisplayName("listTasks：状态名大小写不敏感、前后空白可容忍、数字码照旧")
    void listTasksAcceptsSymbolicAndNumericStatus() {
        when(inspectionTaskRepository.findByConditions(any(), any(), any(), any(), any()))
                .thenReturn(List.of());

        service.listTasks(null, null, "IN_PROGRESS", null, null);
        verify(inspectionTaskRepository).findByConditions(null, null, null, (short) 1, null);

        service.listTasks(null, null, " 3 ", null, null);
        verify(inspectionTaskRepository).findByConditions(null, null, null, (short) 3, null);

        service.listTasks(null, null, "2", null, null);
        verify(inspectionTaskRepository).findByConditions(null, null, null, (short) 2, null);
    }

    @Test
    @DisplayName("listTasks：认不出的 status ⇒ PARAM_ERROR（两个重载共用同一入口），且不查库")
    void listTasksWithUnknownStatusShouldRejectInsteadOfWidening() {
        BusinessException ex =
                assertThrows(
                        BusinessException.class,
                        () -> service.listTasks(null, null, "pending_task", null, null));
        assertEquals(ErrorCode.PARAM_ERROR, ex.getErrorCode());

        assertThrows(
                BusinessException.class,
                () -> service.listTasks(null, null, "??", null, null, 1, 10));

        // 关键词：契约对不上时**报错**，而不是把条件放宽成全表返回 —— 后者正是"设备反复领到已完成任务"的成因。
        verify(inspectionTaskRepository, never())
                .findByConditions(any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("listTasks（分页版）：page/pageSize 非法时回退为 1/10，并回填 total 与 rows")
    void listTasksPageShouldDefaultPagingAndFillTotals() {
        Page<InspectionTask> page = new PageImpl<>(List.of(task(1L, (short) 0)));
        when(inspectionTaskRepository.findByConditions(
                        any(), any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(page);

        InspectionTaskPageDTO dto = service.listTasks(1L, null, null, null, null, 0, -5);

        assertEquals(1L, dto.getTotal());
        assertEquals(1, dto.getRows().size());

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(inspectionTaskRepository)
                .findByConditions(
                        org.mockito.ArgumentMatchers.eq(1L),
                        org.mockito.ArgumentMatchers.isNull(),
                        org.mockito.ArgumentMatchers.isNull(),
                        org.mockito.ArgumentMatchers.isNull(),
                        org.mockito.ArgumentMatchers.isNull(),
                        captor.capture());
        assertEquals(0, captor.getValue().getPageNumber(), "page<=0 应回退到第 1 页（0-based 为 0）");
        assertEquals(10, captor.getValue().getPageSize(), "pageSize<=0 应回退到 10");
    }

    // ---------------------------------------------------------------- updateTaskStatus

    @Test
    @DisplayName("updateTaskStatus：任务不存在 → NOT_FOUND")
    void updateTaskStatusWhenMissingShouldThrowNotFound() {
        when(inspectionTaskRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(BusinessException.class, () -> service.updateTaskStatus(99L, "COMPLETED"));
    }

    @Test
    @DisplayName("updateTaskStatus(COMPLETED)：**没有扫描数据**时用净差兜底补 missing（总数 10 − 已扫 0 = 10）")
    void updateTaskStatusCompletedShouldFallbackCountsWhenNothingScanned() {
        InspectionTask task = task(1L, (short) 1);
        task.setTotalItems(10);
        task.setInspectedItems(0); // 从未扫描 ⇒ 手工完成场景
        when(inspectionTaskRepository.findById(1L)).thenReturn(Optional.of(task));
        // 兜底分支会尝试从明细回填"已扫数"；这里明确返回空，代表确实一条明细都没有
        when(inspectionDetailRepository.findByTaskId(1L)).thenReturn(List.of());

        service.updateTaskStatus(1L, "COMPLETED");

        assertEquals((short) 2, task.getStatus());
        assertEquals(100, task.getProgress(), "完成时进度应置 100");
        assertNotNull(task.getEndTime());
        assertEquals(10, task.getMissingItems(), "没有扫描数据 ⇒ 保守估计为总数 10");
        assertEquals(0, task.getExtraItems());
    }

    @Test
    @DisplayName("updateTaskStatus(COMPLETED)：**已算过**的 missing/extra 不被净差覆盖（决策 13 选项 B 的关键约束）")
    void updateTaskStatusCompletedShouldNotOverwriteExistingCounts() {
        InspectionTask task = task(2L, (short) 1);
        task.setTotalItems(10);
        task.setInspectedItems(4);
        task.setMissingItems(1); // 已由差异分类算出
        task.setExtraItems(2); // 与 missing 并存 —— 修复前这是不可能出现的组合
        when(inspectionTaskRepository.findById(2L)).thenReturn(Optional.of(task));

        service.updateTaskStatus(2L, "COMPLETED");

        assertEquals(1, task.getMissingItems(), "已有的权威统计不应被净差覆盖");
        assertEquals(2, task.getExtraItems(), "missing 与 extra 可以同时非 0");
    }

    @Test
    @DisplayName("updateTaskStatus(IN_PROGRESS)：写 startTime、状态置 1，不写 endTime")
    void updateTaskStatusInProgressShouldSetStartTime() {
        InspectionTask task = task(3L, (short) 0);
        when(inspectionTaskRepository.findById(3L)).thenReturn(Optional.of(task));

        service.updateTaskStatus(3L, "IN_PROGRESS");

        assertEquals((short) 1, task.getStatus());
        assertNotNull(task.getStartTime());
        assertNull(task.getEndTime());
    }

    @Test
    @DisplayName("updateTaskStatus(未知状态)：**状态被写成 0（PENDING）**，不抛异常（现状固定）")
    void updateTaskStatusWithUnknownStatusResetsToPending() {
        InspectionTask task = task(4L, (short) 2);
        when(inspectionTaskRepository.findById(4L)).thenReturn(Optional.of(task));

        service.updateTaskStatus(4L, "SOMETHING_ELSE");

        assertEquals((short) 0, task.getStatus(), "未识别的状态字符串会把任务打回待执行（已登记的现状）");
    }

    // ---------------------------------------------------------------- updateTaskProgress

    @Test
    @DisplayName("updateTaskProgress：progress=100 ⇒ COMPLETED 且补 endTime；同时清 dashboard KPI 缓存")
    void updateTaskProgressTo100ShouldCompleteAndEvictDashboard() {
        InspectionTask task = task(1L, (short) 1);
        when(inspectionTaskRepository.findById(1L)).thenReturn(Optional.of(task));

        service.updateTaskProgress(1L, 100, null);

        assertEquals((short) 2, task.getStatus());
        assertEquals(100, task.getProgress());
        assertNotNull(task.getEndTime());
        verify(stringRedisTemplate).delete("dashboard:kpi");
    }

    @Test
    @DisplayName("updateTaskProgress：0<progress<100 且原为 PENDING ⇒ IN_PROGRESS 并补 startTime")
    void updateTaskProgressToPartialShouldStartTask() {
        InspectionTask task = task(2L, (short) 0);
        when(inspectionTaskRepository.findById(2L)).thenReturn(Optional.of(task));

        service.updateTaskProgress(2L, 30, 5);

        assertEquals((short) 1, task.getStatus());
        assertEquals(30, task.getProgress());
        assertEquals(5, task.getInspectedItems(), "scannedCount 应写入 inspectedItems");
        assertNotNull(task.getStartTime());
        assertNull(task.getEndTime());
    }

    @Test
    @DisplayName("updateTaskProgress：progress=0 不触发状态迁移；不减已有 endTime")
    void updateTaskProgressZeroShouldNotTransition() {
        InspectionTask task = task(3L, (short) 0);
        when(inspectionTaskRepository.findById(3L)).thenReturn(Optional.of(task));

        service.updateTaskProgress(3L, 0, null);

        assertEquals((short) 0, task.getStatus(), "progress=0 不应把任务置为进行中");
        assertNull(task.getStartTime());
    }

    @Test
    @DisplayName("updateTaskProgress：任务不存在 → NOT_FOUND")
    void updateTaskProgressWhenMissingShouldThrowNotFound() {
        when(inspectionTaskRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(BusinessException.class, () -> service.updateTaskProgress(99L, 10, null));
    }

    @Test
    @DisplayName("任务类型名映射：0 ⇒ 定时任务、1 ⇒ 手动任务（纯函数分支）")
    void taskTypeNameMapping() {
        InspectionTask t = task(1L, (short) 0);
        t.setTaskType((short) 0);
        when(inspectionTaskRepository.findById(1L)).thenReturn(Optional.of(t));
        assertEquals("定时任务", service.getTask(1L).getTaskTypeDesc(), "taskType=0 → 定时任务");

        t.setTaskType((short) 1);
        assertEquals("手动任务", service.getTask(1L).getTaskTypeDesc(), "taskType=1 → 手动任务");

        t.setTaskType((short) 9);
        assertEquals("未知类型", service.getTask(1L).getTaskTypeDesc(), "未识别类型 → 未知类型");
    }

    @Test
    @DisplayName("任务状态名映射：0..3 各自有英文码，其它值 → UNKNOWN（纯函数分支）")
    void taskStatusNameMapping() {
        InspectionTask t = task(1L, (short) 0);
        when(inspectionTaskRepository.findById(1L)).thenReturn(Optional.of(t));

        assertEquals("PENDING", service.getTask(1L).getStatusDesc());
        t.setStatus((short) 1);
        assertEquals("IN_PROGRESS", service.getTask(1L).getStatusDesc());
        t.setStatus((short) 2);
        assertEquals("COMPLETED", service.getTask(1L).getStatusDesc());
        t.setStatus((short) 3);
        assertEquals("ABORTED", service.getTask(1L).getStatusDesc());
        t.setStatus((short) 7);
        assertEquals("UNKNOWN", service.getTask(1L).getStatusDesc());
    }
}
