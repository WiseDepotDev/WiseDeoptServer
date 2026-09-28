package com.huicang.wise.application.report;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.huicang.wise.common.api.ErrorCode;
import com.huicang.wise.common.exception.BusinessException;
import com.huicang.wise.domain.report.ReportExportRecord;
import com.huicang.wise.domain.report.ReportTask;
import com.huicang.wise.infrastructure.persistence.repository.report.ReportExportRecordRepository;
import com.huicang.wise.infrastructure.persistence.repository.report.ReportTaskRepository;
import java.time.LocalDateTime;
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
 * 报表应用服务的单元测试：任务创建校验、异步执行的状态机、列表过滤与视图映射。
 *
 * <p>两个刻意写下来的写法要点： ① {@code executeReportTask} 全程复用**同一个可变 {@code ReportTask} 实例**，所以 {@code
 * ArgumentCaptor.getAllValues()} 拿到的多个"捕获值"其实是同一个对象（别名）， 断言"捕获到的中间状态
 * 1"是**假断言**；本测试改为持有实体引用、断言**执行后的终态** + save 调用**次数**。 ② 执行失败路径的注入点是导出记录落库（{@code
 * reportExportRecordRepository.save}）， 而不是 POI 生成 —— 这是唯一能稳定触发 {@code catch} 分支的公开路径。
 */
@ExtendWith(MockitoExtension.class)
class ReportApplicationServiceTest {

    @Mock private ReportTaskRepository reportTaskRepository;
    @Mock private ReportExportRecordRepository reportExportRecordRepository;

    private ReportApplicationService service;

    @BeforeEach
    void setUp() {
        service = new ReportApplicationService(reportTaskRepository, reportExportRecordRepository);
    }

    private ReportTaskCreateRequest request(String name) {
        ReportTaskCreateRequest request = new ReportTaskCreateRequest();
        request.setReportName(name);
        request.setTimeRangeStart(LocalDateTime.now().minusDays(7));
        request.setTimeRangeEnd(LocalDateTime.now());
        return request;
    }

    private ReportTask task(short status) {
        ReportTask entity = new ReportTask();
        entity.setStatus(status);
        entity.setReportName("库存台账");
        return entity;
    }

    // ---------------- 创建任务 ----------------

    @Test
    @DisplayName("创建任务：报表名称为空被拒")
    void createReportTaskRejectsNullName() {
        ReportTaskCreateRequest request = request("库存台账");
        request.setReportName(null);

        BusinessException ex =
                assertThrows(BusinessException.class, () -> service.createReportTask(request));

        assertEquals(ErrorCode.PARAM_ERROR, ex.getErrorCode());
        verify(reportTaskRepository, never()).save(any(ReportTask.class));
    }

    @Test
    @DisplayName("创建任务：起始时间为空被拒")
    void createReportTaskRejectsNullStart() {
        ReportTaskCreateRequest request = request("库存台账");
        request.setTimeRangeStart(null);

        BusinessException ex =
                assertThrows(BusinessException.class, () -> service.createReportTask(request));

        assertEquals(ErrorCode.PARAM_ERROR, ex.getErrorCode());
    }

    @Test
    @DisplayName("创建任务：截止时间为空被拒")
    void createReportTaskRejectsNullEnd() {
        ReportTaskCreateRequest request = request("库存台账");
        request.setTimeRangeEnd(null);

        BusinessException ex =
                assertThrows(BusinessException.class, () -> service.createReportTask(request));

        assertEquals(ErrorCode.PARAM_ERROR, ex.getErrorCode());
    }

    @Test
    @DisplayName("创建任务：成功落库，初始状态为 0（待生成）")
    void createReportTaskSuccess() {
        ReportTaskCreateRequest request = request("库存台账");
        request.setRemark("月度");
        when(reportTaskRepository.save(any(ReportTask.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ReportTaskDTO dto = service.createReportTask(request);

        assertNotNull(dto);
        ArgumentCaptor<ReportTask> captor = ArgumentCaptor.forClass(ReportTask.class);
        verify(reportTaskRepository).save(captor.capture());
        ReportTask saved = captor.getValue();
        assertEquals((short) 0, saved.getStatus());
        assertEquals("库存台账", saved.getReportName());
        assertEquals("月度", saved.getRemark());
        assertNotNull(saved.getCreateTime());
        assertNotNull(saved.getUpdateTime());
    }

    // ---------------- 异步执行 ----------------

    @Test
    @DisplayName("执行任务：任务不存在抛 NOT_FOUND 且不落库")
    void executeReportTaskMissing() {
        when(reportTaskRepository.findById(1L)).thenReturn(Optional.empty());

        BusinessException ex =
                assertThrows(BusinessException.class, () -> service.executeReportTask(1L));

        assertEquals(ErrorCode.NOT_FOUND, ex.getErrorCode());
        verify(reportTaskRepository, never()).save(any(ReportTask.class));
    }

    @Test
    @DisplayName("执行任务：成功后终态为 2，并写出导出记录")
    void executeReportTaskSuccess() {
        ReportTask entity = task((short) 0);
        when(reportTaskRepository.findById(1L)).thenReturn(Optional.of(entity));

        service.executeReportTask(1L);

        // 同一实例被就地改写：只断言终态与调用次数（中间态无法从捕获值区分）
        assertEquals((short) 2, entity.getStatus());
        assertNotNull(entity.getGenerateTime());
        assertEquals(0L, entity.getGenerateFileId());
        verify(reportTaskRepository, times(2)).save(entity);

        ArgumentCaptor<ReportExportRecord> captor =
                ArgumentCaptor.forClass(ReportExportRecord.class);
        verify(reportExportRecordRepository).save(captor.capture());
        ReportExportRecord record = captor.getValue();
        assertEquals("xlsx", record.getExportFormat());
        assertEquals(0L, record.getExportUserId());
        assertEquals(0L, record.getExportFileId());
        assertNotNull(record.getExportTime());
    }

    @Test
    @DisplayName("执行任务：报表数据不来自数据库（现状固定：四个数据源都是写死的演示循环）")
    void executeReportTaskDoesNotQueryBusinessData() {
        ReportTask entity = task((short) 0);
        when(reportTaskRepository.findById(1L)).thenReturn(Optional.of(entity));

        service.executeReportTask(1L);

        verify(reportTaskRepository, never()).findAll();
        verify(reportExportRecordRepository, never()).findByTaskId(any());
    }

    @Test
    @DisplayName("执行任务：导出记录落库失败时终态为 3（失败）")
    void executeReportTaskMarksFailedWhenExportRecordFails() {
        ReportTask entity = task((short) 0);
        when(reportTaskRepository.findById(1L)).thenReturn(Optional.of(entity));
        when(reportExportRecordRepository.save(any(ReportExportRecord.class)))
                .thenThrow(new RuntimeException("导出记录写库失败"));

        service.executeReportTask(1L);

        assertEquals((short) 3, entity.getStatus());
        verify(reportTaskRepository, times(3)).save(entity);
    }

    // ---------------- 列表与详情 ----------------

    @Test
    @DisplayName("任务列表：状态为空时返回全部")
    void listReportTasksWithoutFilter() {
        when(reportTaskRepository.findAll()).thenReturn(List.of(task((short) 0), task((short) 2)));

        assertEquals(2, service.listReportTasks(null).size());
    }

    @Test
    @DisplayName("任务列表：按状态过滤（内存过滤，不走数据库条件）")
    void listReportTasksFiltersByStatus() {
        when(reportTaskRepository.findAll()).thenReturn(List.of(task((short) 0), task((short) 2)));

        List<ReportTaskDTO> rows = service.listReportTasks((short) 2);

        assertEquals(1, rows.size());
        assertEquals((short) 2, rows.get(0).getStatus());
    }

    @Test
    @DisplayName("任务列表：无数据返回空表")
    void listReportTasksEmpty() {
        when(reportTaskRepository.findAll()).thenReturn(List.of());

        assertEquals(0, service.listReportTasks(null).size());
    }

    @Test
    @DisplayName("任务详情：不存在抛 NOT_FOUND")
    void getReportTaskMissing() {
        when(reportTaskRepository.findById(1L)).thenReturn(Optional.empty());

        BusinessException ex =
                assertThrows(BusinessException.class, () -> service.getReportTask(1L));

        assertEquals(ErrorCode.NOT_FOUND, ex.getErrorCode());
    }

    @Test
    @DisplayName("任务详情：命中返回完整视图")
    void getReportTaskFound() {
        ReportTask entity = task((short) 2);
        when(reportTaskRepository.findById(1L)).thenReturn(Optional.of(entity));

        ReportTaskDTO dto = service.getReportTask(1L);

        assertEquals("库存台账", dto.getReportName());
        assertEquals((short) 2, dto.getStatus());
    }

    @Test
    @DisplayName("导出记录：按任务查询并映射")
    void listExportRecordsMapsAll() {
        ReportExportRecord record = new ReportExportRecord();
        record.setExportFormat("xlsx");
        record.setExportUserId(9L);
        when(reportExportRecordRepository.findByTaskId(1L)).thenReturn(List.of(record));

        List<ReportExportRecordDTO> rows = service.listExportRecords(1L);

        assertEquals(1, rows.size());
        assertEquals("xlsx", rows.get(0).getExportFormat());
        assertEquals(9L, rows.get(0).getExportUserId());
    }

    @Test
    @DisplayName("导出记录：无记录返回空表")
    void listExportRecordsEmpty() {
        when(reportExportRecordRepository.findByTaskId(1L)).thenReturn(List.of());

        assertEquals(0, service.listExportRecords(1L).size());
    }
}
