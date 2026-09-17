package com.huicang.wise.application.report;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.huicang.wise.common.api.ErrorCode;
import com.huicang.wise.common.exception.BusinessException;
import com.huicang.wise.domain.report.ReportExportRecord;
import com.huicang.wise.domain.report.ReportTask;
import com.huicang.wise.domain.repository.report.ReportExportRecordRepository;
import com.huicang.wise.domain.repository.report.ReportTaskRepository;
import com.huicang.wise.infrastructure.redis.annotation.Cacheable;

/**
 * 类功能描述：报表应用服务
 *
 * @author WiseDepot
 * @version 0.1.18
 * @since 2026-02-27
 */
@Service
public class ReportApplicationService {

    private final ReportTaskRepository reportTaskRepository;
    private final ReportExportRecordRepository reportExportRecordRepository;

    public ReportApplicationService(ReportTaskRepository reportTaskRepository,
                                  ReportExportRecordRepository reportExportRecordRepository) {
        this.reportTaskRepository = reportTaskRepository;
        this.reportExportRecordRepository = reportExportRecordRepository;
    }

    /**
     * 方法功能描述：创建报表任务
     *
     * @param request 报表任务创建请求
     * @return 报表任务
     */
    @Transactional
    public ReportTaskDTO createReportTask(ReportTaskCreateRequest request) {
        if (request.getReportName() == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "报表名称不能为空");
        }
        if (request.getTimeRangeStart() == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "数据起始时间不能为空");
        }
        if (request.getTimeRangeEnd() == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "数据截止时间不能为空");
        }

        ReportTask entity = new ReportTask();
        entity.setReportName(request.getReportName());
        entity.setTimeRangeStart(request.getTimeRangeStart());
        entity.setTimeRangeEnd(request.getTimeRangeEnd());
        entity.setStatus((short) 0);
        entity.setRemark(request.getRemark());
        entity.setCreateTime(LocalDateTime.now());
        entity.setUpdateTime(LocalDateTime.now());

        ReportTask saved = reportTaskRepository.save(entity);
        
        return toReportTaskDTO(saved);
    }

    /**
     * 方法功能描述：异步执行报表任务
     *
     * @param taskId 任务ID
     */
    @Async
    @Transactional
    public void executeReportTask(Long taskId) {
        ReportTask task = reportTaskRepository.findById(taskId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "报表任务不存在"));

        try {
            task.setStatus((short) 1);
            reportTaskRepository.save(task);

            byte[] fileData = generateInventoryReport(task);
            String fileName = "库存台账报表_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".xlsx";

            task.setStatus((short) 2);
            task.setGenerateTime(LocalDateTime.now());
            task.setGenerateFileId(0L);
            reportTaskRepository.save(task);

            createExportRecord(task.getTaskId(), fileName, Long.valueOf(fileData.length), 0, 0L);

        } catch (Exception e) {
            task.setStatus((short) 3);
            reportTaskRepository.save(task);
        }
    }

    /**
     * 方法功能描述：生成库存台账报表
     *
     * @param task 报表任务
     * @return 报表数据
     */
    private byte[] generateInventoryReport(ReportTask task) throws IOException {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("库存台账");

        Row headerRow = sheet.createRow(0);
        String[] headers = {"产品编码", "产品名称", "规格型号", "当前库存", "单位", "仓库", "库位", "最后更新时间"};
        for (int i = 0; i < headers.length; i++) {
            headerRow.createCell(i).setCellValue(headers[i]);
        }

        List<Map<String, Object>> inventoryData = getInventoryData(task);
        for (int i = 0; i < inventoryData.size(); i++) {
            Row row = sheet.createRow(i + 1);
            Map<String, Object> item = inventoryData.get(i);
            row.createCell(0).setCellValue((String) item.get("productCode"));
            row.createCell(1).setCellValue((String) item.get("productName"));
            row.createCell(2).setCellValue((String) item.get("specification"));
            row.createCell(3).setCellValue(((Number) item.get("quantity")).intValue());
            row.createCell(4).setCellValue((String) item.get("unit"));
            row.createCell(5).setCellValue((String) item.get("warehouseName"));
            row.createCell(6).setCellValue((String) item.get("location"));
            row.createCell(7).setCellValue(item.get("updateTime").toString());
        }

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        workbook.write(outputStream);
        workbook.close();
        return outputStream.toByteArray();
    }

    /**
     * 方法功能描述：生成对账报表
     *
     * @param task 报表任务
     * @return 报表数据
     */
    private byte[] generateReconciliationReport(ReportTask task) throws IOException {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("对账记录");

        Row headerRow = sheet.createRow(0);
        String[] headers = {"单据编号", "单据类型", "产品编码", "产品名称", "数量", "仓库", "状态", "创建时间"};
        for (int i = 0; i < headers.length; i++) {
            headerRow.createCell(i).setCellValue(headers[i]);
        }

        List<Map<String, Object>> reconciliationData = getReconciliationData(task);
        for (int i = 0; i < reconciliationData.size(); i++) {
            Row row = sheet.createRow(i + 1);
            Map<String, Object> item = reconciliationData.get(i);
            row.createCell(0).setCellValue((String) item.get("orderCode"));
            row.createCell(1).setCellValue((String) item.get("orderType"));
            row.createCell(2).setCellValue((String) item.get("productCode"));
            row.createCell(3).setCellValue((String) item.get("productName"));
            row.createCell(4).setCellValue(((Number) item.get("quantity")).intValue());
            row.createCell(5).setCellValue((String) item.get("warehouseName"));
            row.createCell(6).setCellValue((String) item.get("status"));
            row.createCell(7).setCellValue(item.get("createTime").toString());
        }

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        workbook.write(outputStream);
        workbook.close();
        return outputStream.toByteArray();
    }

    /**
     * 方法功能描述：生成差异分析报表
     *
     * @param task 报表任务
     * @return 报表数据
     */
    private byte[] generateDifferenceReport(ReportTask task) throws IOException {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("差异分析");

        Row headerRow = sheet.createRow(0);
        String[] headers = {"产品编码", "产品名称", "预期数量", "实际数量", "差异数量", "差异类型", "位置", "巡检时间"};
        for (int i = 0; i < headers.length; i++) {
            headerRow.createCell(i).setCellValue(headers[i]);
        }

        List<Map<String, Object>> differenceData = getDifferenceData(task);
        for (int i = 0; i < differenceData.size(); i++) {
            Row row = sheet.createRow(i + 1);
            Map<String, Object> item = differenceData.get(i);
            row.createCell(0).setCellValue((String) item.get("productCode"));
            row.createCell(1).setCellValue((String) item.get("productName"));
            row.createCell(2).setCellValue(((Number) item.get("expectedQuantity")).intValue());
            row.createCell(3).setCellValue(((Number) item.get("actualQuantity")).intValue());
            row.createCell(4).setCellValue(((Number) item.get("differenceQuantity")).intValue());
            row.createCell(5).setCellValue((String) item.get("differenceType"));
            row.createCell(6).setCellValue((String) item.get("location"));
            row.createCell(7).setCellValue(item.get("inspectionTime").toString());
        }

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        workbook.write(outputStream);
        workbook.close();
        return outputStream.toByteArray();
    }

    /**
     * 方法功能描述：生成巡检报表
     *
     * @param task 报表任务
     * @return 报表数据
     */
    private byte[] generateInspectionReport(ReportTask task) throws IOException {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("巡检记录");

        Row headerRow = sheet.createRow(0);
        String[] headers = {"任务编号", "计划名称", "仓库", "巡检时间", "总项数", "正常项数", "异常项数", "准确率", "状态"};
        for (int i = 0; i < headers.length; i++) {
            headerRow.createCell(i).setCellValue(headers[i]);
        }

        List<Map<String, Object>> inspectionData = getInspectionData(task);
        for (int i = 0; i < inspectionData.size(); i++) {
            Row row = sheet.createRow(i + 1);
            Map<String, Object> item = inspectionData.get(i);
            row.createCell(0).setCellValue((String) item.get("taskCode"));
            row.createCell(1).setCellValue((String) item.get("planName"));
            row.createCell(2).setCellValue((String) item.get("warehouseName"));
            row.createCell(3).setCellValue(item.get("inspectionTime").toString());
            row.createCell(4).setCellValue(((Number) item.get("totalItems")).intValue());
            row.createCell(5).setCellValue(((Number) item.get("normalItems")).intValue());
            row.createCell(6).setCellValue(((Number) item.get("abnormalItems")).intValue());
            row.createCell(7).setCellValue(((Number) item.get("accuracyRate")).doubleValue());
            row.createCell(8).setCellValue((String) item.get("status"));
        }

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        workbook.write(outputStream);
        workbook.close();
        return outputStream.toByteArray();
    }

    /**
     * 方法功能描述：获取库存数据
     *
     * @param task 报表任务
     * @return 库存数据
     */
    private List<Map<String, Object>> getInventoryData(ReportTask task) {
        List<Map<String, Object>> data = new ArrayList<>();
        for (int i = 1; i <= 10; i++) {
            Map<String, Object> item = new HashMap<>();
            item.put("productCode", "P" + String.format("%04d", i));
            item.put("productName", "产品" + i);
            item.put("specification", "规格" + i);
            item.put("quantity", 100 + i * 10);
            item.put("unit", "个");
            item.put("warehouseName", "主仓库");
            item.put("location", "A区" + i + "排");
            item.put("updateTime", LocalDateTime.now());
            data.add(item);
        }
        return data;
    }

    /**
     * 方法功能描述：获取对账数据
     *
     * @param task 报表任务
     * @return 对账数据
     */
    private List<Map<String, Object>> getReconciliationData(ReportTask task) {
        List<Map<String, Object>> data = new ArrayList<>();
        for (int i = 1; i <= 10; i++) {
            Map<String, Object> item = new HashMap<>();
            item.put("orderCode", "ORD" + String.format("%04d", i));
            item.put("orderType", i % 2 == 0 ? "入库" : "出库");
            item.put("productCode", "P" + String.format("%04d", i));
            item.put("productName", "产品" + i);
            item.put("quantity", 10 + i);
            item.put("warehouseName", "主仓库");
            item.put("status", i % 3 == 0 ? "已完成" : "处理中");
            item.put("createTime", LocalDateTime.now());
            data.add(item);
        }
        return data;
    }

    /**
     * 方法功能描述：获取差异数据
     *
     * @param task 报表任务
     * @return 差异数据
     */
    private List<Map<String, Object>> getDifferenceData(ReportTask task) {
        List<Map<String, Object>> data = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            Map<String, Object> item = new HashMap<>();
            item.put("productCode", "P" + String.format("%04d", i));
            item.put("productName", "产品" + i);
            item.put("expectedQuantity", 100);
            item.put("actualQuantity", 95 + i);
            item.put("differenceQuantity", 5 - i);
            item.put("differenceType", i % 2 == 0 ? "缺失" : "多余");
            item.put("location", "A区" + i + "排");
            item.put("inspectionTime", LocalDateTime.now());
            data.add(item);
        }
        return data;
    }

    /**
     * 方法功能描述：获取巡检数据
     *
     * @param task 报表任务
     * @return 巡检数据
     */
    private List<Map<String, Object>> getInspectionData(ReportTask task) {
        List<Map<String, Object>> data = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            Map<String, Object> item = new HashMap<>();
            item.put("taskCode", "TSK" + String.format("%04d", i));
            item.put("planName", "巡检计划" + i);
            item.put("warehouseName", "主仓库");
            item.put("inspectionTime", LocalDateTime.now());
            item.put("totalItems", 100 + i * 10);
            item.put("normalItems", 95 + i * 10);
            item.put("abnormalItems", 5);
            item.put("accuracyRate", 95.0 + i * 0.5);
            item.put("status", "已完成");
            data.add(item);
        }
        return data;
    }

    /**
     * 方法功能描述：创建导出记录
     *
     * @param taskId 任务ID
     * @param fileName 文件名称
     * @param fileSize 文件大小
     * @param format 文件格式
     * @param createBy 创建者ID
     */
    private void createExportRecord(Long taskId, String fileName, Long fileSize, Integer format, Long createBy) {
        ReportExportRecord record = new ReportExportRecord();
        record.setTaskId(taskId);
        record.setExportUserId(createBy);
        record.setExportFormat(getFileExtension(format).substring(1));
        record.setExportTime(LocalDateTime.now());
        record.setExportFileId(0L);
        reportExportRecordRepository.save(record);
    }

    /**
     * 方法功能描述：获取文件扩展名
     *
     * @param format 文件格式
     * @return 文件扩展名
     */
    private String getFileExtension(Integer format) {
        switch (format) {
            case 0:
                return ".xlsx";
            case 1:
                return ".pdf";
            case 2:
                return ".csv";
            default:
                return ".xlsx";
        }
    }

    /**
     * 方法功能描述：查询报表任务列表
     *
     * @param reportType 报表类型
     * @param status 任务状态
     * @param createBy 创建者ID
     * @return 报表任务列表
     */
    public List<ReportTaskDTO> listReportTasks(Short status) {
        List<ReportTask> entities = reportTaskRepository.findAll().stream()
                .filter(e -> status == null || status.equals(e.getStatus()))
                .collect(java.util.stream.Collectors.toList());

        return entities.stream().map(this::toReportTaskDTO).collect(java.util.stream.Collectors.toList());
    }

    /**
     * 方法功能描述：查询报表任务详情
     *
     * @param taskId 任务ID
     * @return 报表任务
     */
    @Cacheable(prefix = "report:task", key = "#taskId", timeout = 1800)
    public ReportTaskDTO getReportTask(Long taskId) {
        ReportTask entity = reportTaskRepository.findById(taskId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "报表任务不存在"));
        return toReportTaskDTO(entity);
    }

    /**
     * 方法功能描述：查询导出记录列表
     *
     * @param taskId 任务ID
     * @return 导出记录列表
     */
    public List<ReportExportRecordDTO> listExportRecords(Long taskId) {
        List<ReportExportRecord> entities = reportExportRecordRepository.findByTaskId(taskId);
        return entities.stream().map(this::toReportExportRecordDTO).collect(java.util.stream.Collectors.toList());
    }

    /**
     * 方法功能描述：转换为报表任务DTO
     *
     * @param entity 实体
     * @return DTO
     */
    private ReportTaskDTO toReportTaskDTO(ReportTask entity) {
        ReportTaskDTO dto = new ReportTaskDTO();
        dto.setTaskId(entity.getTaskId());
        dto.setReportName(entity.getReportName());
        dto.setTimeRangeStart(entity.getTimeRangeStart());
        dto.setTimeRangeEnd(entity.getTimeRangeEnd());
        dto.setStatus(entity.getStatus());
        dto.setGenerateTime(entity.getGenerateTime());
        dto.setGenerateFileId(entity.getGenerateFileId());
        dto.setRemark(entity.getRemark());
        dto.setCreateTime(entity.getCreateTime());
        dto.setUpdateTime(entity.getUpdateTime());
        return dto;
    }

    /**
     * 方法功能描述：转换为导出记录DTO
     *
     * @param entity 实体
     * @return DTO
     */
    private ReportExportRecordDTO toReportExportRecordDTO(ReportExportRecord entity) {
        ReportExportRecordDTO dto = new ReportExportRecordDTO();
        dto.setRecordId(entity.getRecordId());
        dto.setTaskId(entity.getTaskId());
        dto.setExportUserId(entity.getExportUserId());
        dto.setExportFormat(entity.getExportFormat());
        dto.setExportTime(entity.getExportTime());
        dto.setExportFileId(entity.getExportFileId());
        dto.setRemark(entity.getRemark());
        return dto;
    }
}