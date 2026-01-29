package com.huicang.wise.application.inspection;

import com.huicang.wise.application.alert.AlertApplicationService;
import com.huicang.wise.application.alert.AlertCreateRequest;
import com.huicang.wise.common.api.ErrorCode;
import com.huicang.wise.common.exception.BusinessException;
import com.huicang.wise.infrastructure.repository.device.DeviceCoreJpaEntity;
import com.huicang.wise.infrastructure.repository.device.DeviceCoreRepository;
import com.huicang.wise.infrastructure.repository.inspection.*;
import com.huicang.wise.infrastructure.repository.inventory.InventoryDifferenceJpaEntity;
import com.huicang.wise.infrastructure.repository.inventory.InventoryDifferenceRepository;
import com.huicang.wise.infrastructure.repository.inventory.InventoryJpaEntity;
import com.huicang.wise.infrastructure.repository.inventory.InventoryRepository;
import com.huicang.wise.infrastructure.repository.inventory.ProductRepository;
import com.huicang.wise.infrastructure.repository.tag.ProductTagJpaEntity;
import com.huicang.wise.infrastructure.repository.tag.ProductTagRepository;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 巡检应用服务
 *
 * @author B1
 * @version 1.0
 * @since 2024-04-20
 * @modified 2026-01-28 实现缺失项计算和统计
 */
@Service
public class InspectionApplicationService {

    private final InspectionPlanRepository inspectionPlanRepository;
    private final InspectionTaskRepository inspectionTaskRepository;
    private final InspectionDetailRepository inspectionDetailRepository;
    private final InspectionResultRepository inspectionResultRepository;
    private final DeviceCoreRepository deviceCoreRepository;
    private final ProductTagRepository productTagRepository;
    private final ProductRepository productRepository;
    private final InventoryRepository inventoryRepository;
    private final InventoryDifferenceRepository inventoryDifferenceRepository;
    private final AlertApplicationService alertApplicationService;
    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();

    public InspectionApplicationService(InspectionPlanRepository inspectionPlanRepository,
                                        InspectionTaskRepository inspectionTaskRepository,
                                        InspectionDetailRepository inspectionDetailRepository,
                                        InspectionResultRepository inspectionResultRepository,
                                        DeviceCoreRepository deviceCoreRepository,
                                        ProductTagRepository productTagRepository,
                                        ProductRepository productRepository,
                                        InventoryRepository inventoryRepository,
                                        InventoryDifferenceRepository inventoryDifferenceRepository,
                                        AlertApplicationService alertApplicationService) {
        this.inspectionPlanRepository = inspectionPlanRepository;
        this.inspectionTaskRepository = inspectionTaskRepository;
        this.inspectionDetailRepository = inspectionDetailRepository;
        this.inspectionResultRepository = inspectionResultRepository;
        this.deviceCoreRepository = deviceCoreRepository;
        this.productTagRepository = productTagRepository;
        this.productRepository = productRepository;
        this.inventoryRepository = inventoryRepository;
        this.inventoryDifferenceRepository = inventoryDifferenceRepository;
        this.alertApplicationService = alertApplicationService;
    }

    // --- 巡检计划管理 ---

    @Transactional
    public InspectionPlanVO createPlan(CreateInspectionPlanDTO request) throws BusinessException {
        if (request.getDeviceId() != null) {
            deviceCoreRepository.findById(request.getDeviceId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.PARAM_ERROR, "设备不存在"));
        }

        InspectionPlanJpaEntity entity = new InspectionPlanJpaEntity();
        entity.setPlanId(System.currentTimeMillis());
        entity.setPlanName(request.getPlanName());
        entity.setDeviceId(request.getDeviceId());
        entity.setCronExpression(request.getCronExpression());
        entity.setRouteData(request.getRouteData());
        entity.setStatus(1); // 默认启用
        entity.setCreateTime(LocalDateTime.now());
        
        InspectionPlanJpaEntity saved = inspectionPlanRepository.save(entity);
        return toPlanVO(saved);
    }

    @Transactional
    public InspectionPlanVO updatePlan(UpdateInspectionPlanDTO request) throws BusinessException {
        InspectionPlanJpaEntity entity = inspectionPlanRepository.findById(request.getPlanId())
                .orElseThrow(() -> new BusinessException(ErrorCode.PARAM_ERROR, "计划不存在"));

        if (request.getPlanName() != null) entity.setPlanName(request.getPlanName());
        if (request.getDeviceId() != null) {
            deviceCoreRepository.findById(request.getDeviceId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.PARAM_ERROR, "设备不存在"));
            entity.setDeviceId(request.getDeviceId());
        }
        if (request.getCronExpression() != null) entity.setCronExpression(request.getCronExpression());
        if (request.getRouteData() != null) entity.setRouteData(request.getRouteData());
        if (request.getStatus() != null) entity.setStatus(request.getStatus());

        return toPlanVO(inspectionPlanRepository.save(entity));
    }

    public void deletePlan(Long planId) {
        inspectionPlanRepository.deleteById(planId);
    }

    public InspectionPlanVO getPlan(Long planId) throws BusinessException {
        return toPlanVO(inspectionPlanRepository.findById(planId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PARAM_ERROR, "计划不存在")));
    }

    public List<InspectionPlanVO> listPlans() {
        return inspectionPlanRepository.findAll().stream()
                .map(this::toPlanVO)
                .collect(Collectors.toList());
    }

    @Transactional
    public void updatePlanStatus(Long planId, Integer status) throws BusinessException {
        InspectionPlanJpaEntity entity = inspectionPlanRepository.findById(planId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PARAM_ERROR, "计划不存在"));
        entity.setStatus(status);
        inspectionPlanRepository.save(entity);
    }

    // --- 巡检任务管理 ---

    @Transactional
    public InspectionTaskVO createManualTask(Long deviceId) throws BusinessException {
        DeviceCoreJpaEntity device = deviceCoreRepository.findById(deviceId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PARAM_ERROR, "设备不存在"));

        InspectionTaskJpaEntity task = new InspectionTaskJpaEntity();
        task.setTaskId(System.currentTimeMillis());
        task.setPlanId(0L); // 手动任务没有计划ID
        task.setTaskType(1); // 手动
        task.setDeviceId(deviceId);
        task.setStatus(0); // 待执行
        task.setCreateTime(LocalDateTime.now());

        return toTaskVO(inspectionTaskRepository.save(task));
    }

    public List<InspectionTaskVO> listTasks(Long planId, Integer taskType, Long deviceId, Integer status) {
        // 简化实现：这里应该使用Specification或Querydsl进行动态查询，为节省时间先用过滤
        return inspectionTaskRepository.findAll().stream()
                .filter(t -> planId == null || planId.equals(t.getPlanId()))
                .filter(t -> taskType == null || taskType.equals(t.getTaskType()))
                .filter(t -> deviceId == null || deviceId.equals(t.getDeviceId()))
                .filter(t -> status == null || status.equals(t.getStatus()))
                .map(this::toTaskVO)
                .collect(Collectors.toList());
    }

    public List<InspectionTaskVO> listPendingTasksForDevice(Long deviceId) {
        return inspectionTaskRepository.findByDeviceIdAndStatus(deviceId, 0).stream()
                .map(this::toTaskVO)
                .collect(Collectors.toList());
    }

    public List<InspectionTaskVO> listHistoryTasksForDevice(Long deviceId) {
         // 实际应分页，这里简化
        return inspectionTaskRepository.findByDeviceId(deviceId).stream()
                .filter(t -> t.getStatus() != 0)
                .map(this::toTaskVO)
                .collect(Collectors.toList());
    }

    @Transactional
    public InspectionTaskVO startTask(Long taskId) throws BusinessException {
        InspectionTaskJpaEntity task = inspectionTaskRepository.findById(taskId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PARAM_ERROR, "任务不存在"));
        
        if (task.getStatus() != 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "任务状态不正确，无法开始");
        }

        task.setStatus(1); // 执行中
        task.setStartTime(LocalDateTime.now());
        return toTaskVO(inspectionTaskRepository.save(task));
    }

    @Transactional
    public InspectionTaskVO completeTask(Long taskId) throws BusinessException {
        InspectionTaskJpaEntity task = inspectionTaskRepository.findById(taskId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PARAM_ERROR, "任务不存在"));

        if (task.getStatus() != 1) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "任务状态不正确，无法完成");
        }

        task.setStatus(2); // 已完成
        task.setEndTime(LocalDateTime.now());
        InspectionTaskJpaEntity saved = inspectionTaskRepository.save(task);
        
        // 触发结果处理（可以是异步的，这里简化为同步调用或者留给process接口）
        processResult(taskId); 
        
        return toTaskVO(saved);
    }

    // --- 巡检明细上报 ---

    @Transactional
    public void reportDetails(List<InspectionDetailDTO> details) {
        if (details == null || details.isEmpty()) return;

        List<InspectionDetailJpaEntity> entities = details.stream().map(dto -> {
            InspectionDetailJpaEntity entity = new InspectionDetailJpaEntity();
            entity.setDetailId(System.nanoTime()); // 简单ID生成
            entity.setTaskId(dto.getTaskId());
            entity.setRfid(dto.getRfid());
            entity.setScanTime(dto.getScanTime() != null ? dto.getScanTime() : LocalDateTime.now());
            
            // 尝试查找tagId
            ProductTagJpaEntity tag = productTagRepository.findByRfid(dto.getRfid());
            if (tag != null) {
                entity.setTagId(tag.getTagId());
                entity.setMatched(1);
            } else {
                entity.setTagId(0L);
                entity.setMatched(0);
            }
            entity.setRemark(dto.getRemark());
            return entity;
        }).collect(Collectors.toList());

        inspectionDetailRepository.saveAll(entities);
    }

    // --- 结果处理 ---

    @Transactional
    public void processResult(Long taskId) throws BusinessException {
        InspectionTaskJpaEntity task = inspectionTaskRepository.findById(taskId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PARAM_ERROR, "任务不存在"));

        if (task.getStatus() != 2) {
             throw new BusinessException(ErrorCode.PARAM_ERROR, "任务未完成，无法生成结果");
        }

        // 1. 获取所有预期在库的标签 (假设状态"1"为在库)
        List<ProductTagJpaEntity> expectedTags = productTagRepository.findByStatus("1");
        Map<String, ProductTagJpaEntity> expectedMap = expectedTags.stream()
                .collect(Collectors.toMap(ProductTagJpaEntity::getRfid, t -> t, (k1, k2) -> k1));
        
        // 2. 获取本次巡检扫描到的明细
        List<InspectionDetailJpaEntity> scannedDetails = inspectionDetailRepository.findByTaskId(taskId);
        Set<String> scannedRfids = scannedDetails.stream()
                .map(InspectionDetailJpaEntity::getRfid)
                .collect(Collectors.toSet());

        // 3. 计算匹配、缺失、多余
        int totalExpected = expectedTags.size();
        int totalScanned = scannedDetails.size();
        
        List<String> matchedRfids = new ArrayList<>();
        List<String> missingRfids = new ArrayList<>();
        List<String> extraRfids = new ArrayList<>();

        for (String rfid : expectedMap.keySet()) {
            if (scannedRfids.contains(rfid)) {
                matchedRfids.add(rfid);
            } else {
                missingRfids.add(rfid);
            }
        }

        for (String rfid : scannedRfids) {
            if (!expectedMap.containsKey(rfid)) {
                extraRfids.add(rfid);
            }
        }

        int matchedCount = matchedRfids.size();
        int missingCount = missingRfids.size();
        int extraCount = extraRfids.size();

        // 4. 生成结果实体
        InspectionResultJpaEntity result = new InspectionResultJpaEntity();
        result.setResultId(System.currentTimeMillis());
        result.setTaskId(taskId);
        result.setTotalExpected(totalExpected);
        result.setTotalScanned(totalScanned);
        result.setMatchedCount(matchedCount);
        result.setMissingCount(missingCount);
        result.setExtraCount(extraCount);
        result.setCompareTime(LocalDateTime.now());
        result.setCreateTime(LocalDateTime.now());

        // 5. 序列化详情数据
        try {
            // 构造缺失项详情列表
            List<Map<String, Object>> missingList = new ArrayList<>();
            for (String rfid : missingRfids) {
                ProductTagJpaEntity tag = expectedMap.get(rfid);
                Map<String, Object> item = new HashMap<>();
                item.put("rfid", rfid);
                item.put("productId", tag.getProductId());
                // 查询产品名称
                productRepository.findById(tag.getProductId()).ifPresent(p -> item.put("productName", p.getProductName()));
                missingList.add(item);
            }
            result.setMissingProducts(objectMapper.writeValueAsString(missingList));

            // 构造多余项详情列表
            List<Map<String, Object>> extraList = new ArrayList<>();
            for (String rfid : extraRfids) {
                Map<String, Object> item = new HashMap<>();
                item.put("rfid", rfid);
                extraList.add(item);
            }
            result.setExtraProducts(objectMapper.writeValueAsString(extraList));
            
        } catch (Exception e) {
            e.printStackTrace();
            result.setMissingProducts("[]");
            result.setExtraProducts("[]");
        }
        
        inspectionResultRepository.save(result);

        // 5.5 生成库存差异记录
        Map<Long, List<String>> missingByProduct = new HashMap<>();
        for (String rfid : missingRfids) {
            ProductTagJpaEntity tag = expectedMap.get(rfid);
            if (tag != null) {
                missingByProduct.computeIfAbsent(tag.getProductId(), k -> new ArrayList<>()).add(rfid);
            }
        }
        
        for (Map.Entry<Long, List<String>> entry : missingByProduct.entrySet()) {
             Long productId = entry.getKey();
             int count = entry.getValue().size();
             
             InventoryDifferenceJpaEntity diff = new InventoryDifferenceJpaEntity();
             diff.setDiffId(System.nanoTime() + productId); 
             diff.setTaskId(taskId);
             diff.setProductId(productId);
             productRepository.findById(productId).ifPresent(p -> diff.setProductName(p.getProductName()));
             
             List<InventoryJpaEntity> inventories = inventoryRepository.findByProductId(productId);
             if (!inventories.isEmpty()) {
                 InventoryJpaEntity inv = inventories.get(0);
                 diff.setLocationCode(inv.getLocationCode());
                 diff.setExpectedQuantity(inv.getQuantity());
                 diff.setActualQuantity(Math.max(0, inv.getQuantity() - count));
             } else {
                 diff.setLocationCode("UNKNOWN");
                 diff.setExpectedQuantity(count);
                 diff.setActualQuantity(0);
             }
             
             diff.setDiffType("MISSING");
             diff.setStatus(0);
             diff.setCreatedAt(LocalDateTime.now());
             diff.setUpdatedAt(LocalDateTime.now());
             inventoryDifferenceRepository.save(diff);
        }

        Map<Long, List<String>> extraByProduct = new HashMap<>();
        for (String rfid : extraRfids) {
            ProductTagJpaEntity tag = productTagRepository.findByRfid(rfid);
            if (tag != null) {
                extraByProduct.computeIfAbsent(tag.getProductId(), k -> new ArrayList<>()).add(rfid);
            }
        }
        
        for (Map.Entry<Long, List<String>> entry : extraByProduct.entrySet()) {
             Long productId = entry.getKey();
             int count = entry.getValue().size();
             
             InventoryDifferenceJpaEntity diff = new InventoryDifferenceJpaEntity();
             diff.setDiffId(System.nanoTime() + productId + 10000); 
             diff.setTaskId(taskId);
             diff.setProductId(productId);
             productRepository.findById(productId).ifPresent(p -> diff.setProductName(p.getProductName()));
             
             List<InventoryJpaEntity> inventories = inventoryRepository.findByProductId(productId);
             if (!inventories.isEmpty()) {
                 InventoryJpaEntity inv = inventories.get(0);
                 diff.setLocationCode(inv.getLocationCode());
                 diff.setExpectedQuantity(inv.getQuantity());
                 diff.setActualQuantity(inv.getQuantity() + count);
             } else {
                 diff.setLocationCode("UNKNOWN");
                 diff.setExpectedQuantity(0);
                 diff.setActualQuantity(count);
             }
             
             diff.setDiffType("SURPLUS");
             diff.setStatus(0);
             diff.setCreatedAt(LocalDateTime.now());
             diff.setUpdatedAt(LocalDateTime.now());
             inventoryDifferenceRepository.save(diff);
        }

        // 6. 触发告警
        if (missingCount > 0 || extraCount > 0) {
            try {
                AlertCreateRequest alertRequest = new AlertCreateRequest();
                alertRequest.setAlertType("INSPECTION_EXCEPTION");
                alertRequest.setAlertLevel("HIGH");
                alertRequest.setDescription(String.format("巡检任务 %d 异常：缺失 %d，多余 %d", taskId, missingCount, extraCount));
                alertApplicationService.createAlert(alertRequest);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    
    public InspectionStatisticsVO getStatistics() {
        InspectionStatisticsVO vo = new InspectionStatisticsVO();
        
        // 简单统计，实际应基于数据库查询
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        List<InspectionTaskJpaEntity> allTasks = inspectionTaskRepository.findAll();
        
        long todayTaskCount = allTasks.stream()
                .filter(t -> t.getCreateTime().isAfter(todayStart))
                .count();
        long todayCompletedCount = allTasks.stream()
                .filter(t -> t.getCreateTime().isAfter(todayStart) && t.getStatus() == 2)
                .count();
                
        // 查找最近一个完成的任务的结果
        Optional<InspectionResultJpaEntity> lastResult = inspectionResultRepository.findAll().stream()
                .sorted(Comparator.comparing(InspectionResultJpaEntity::getCreateTime).reversed())
                .findFirst();
                
        vo.setTodayTaskCount((int) todayTaskCount);
        vo.setTodayCompletedCount((int) todayCompletedCount);
        vo.setAbnormalTaskCount(0); // 暂不统计
        
        if (lastResult.isPresent()) {
            vo.setLastMissingCount(lastResult.get().getMissingCount());
            vo.setLastExtraCount(lastResult.get().getExtraCount());
        } else {
            vo.setLastMissingCount(0);
            vo.setLastExtraCount(0);
        }
        
        return vo;
    }
    
    public List<InspectionMissingItemVO> getMissingItems(Long taskId) {
        Optional<InspectionResultJpaEntity> resultOpt = inspectionResultRepository.findByTaskId(taskId);
        if (resultOpt.isEmpty()) return Collections.emptyList();
        
        String json = resultOpt.get().getMissingProducts();
        if (json == null || json.isEmpty()) return Collections.emptyList();
        
        try {
            List<Map<String, Object>> list = objectMapper.readValue(json, new com.fasterxml.jackson.core.type.TypeReference<List<Map<String, Object>>>(){});
            return list.stream().map(m -> {
                InspectionMissingItemVO vo = new InspectionMissingItemVO();
                vo.setRfid((String) m.get("rfid"));
                if (m.get("productId") != null) vo.setProductId(((Number) m.get("productId")).longValue());
                vo.setProductName((String) m.get("productName"));
                return vo;
            }).collect(Collectors.toList());
        } catch (Exception e) {
            e.printStackTrace();
            return Collections.emptyList();
        }
    }

    public InspectionResultSummaryVO getResultSummary(Long taskId) {
        return inspectionResultRepository.findByTaskId(taskId)
                .map(this::toResultVO)
                .orElse(null);
    }
    
    public List<InspectionDetailVO> listDetails(Long taskId) {
        return inspectionDetailRepository.findByTaskId(taskId).stream()
                .map(this::toDetailVO)
                .collect(Collectors.toList());
    }

    // --- Helpers ---

    private InspectionPlanVO toPlanVO(InspectionPlanJpaEntity entity) {
        InspectionPlanVO vo = new InspectionPlanVO();
        BeanUtils.copyProperties(entity, vo);
        
        if (entity.getDeviceId() != null) {
            deviceCoreRepository.findById(entity.getDeviceId())
                    .ifPresent(d -> vo.setDeviceName(d.getName()));
        }
        vo.setStatusDescription(entity.getStatus() == 1 ? "启用" : "禁用");
        return vo;
    }

    private InspectionTaskVO toTaskVO(InspectionTaskJpaEntity entity) {
        InspectionTaskVO vo = new InspectionTaskVO();
        BeanUtils.copyProperties(entity, vo);
        
        if (entity.getPlanId() != null && entity.getPlanId() > 0) {
            inspectionPlanRepository.findById(entity.getPlanId())
                    .ifPresent(p -> vo.setPlanName(p.getPlanName()));
        }
        
        if (entity.getDeviceId() != null) {
            deviceCoreRepository.findById(entity.getDeviceId())
                    .ifPresent(d -> vo.setDeviceName(d.getName()));
        }
        
        vo.setTaskTypeDescription(entity.getTaskType() == 0 ? "定时任务" : "手动任务");
        
        String statusDesc;
        switch (entity.getStatus()) {
            case 0: statusDesc = "待执行"; break;
            case 1: statusDesc = "执行中"; break;
            case 2: statusDesc = "已完成"; break;
            case 3: statusDesc = "异常终止"; break;
            default: statusDesc = "未知";
        }
        vo.setStatusDescription(statusDesc);
        
        return vo;
    }

    private InspectionDetailVO toDetailVO(InspectionDetailJpaEntity entity) {
        InspectionDetailVO vo = new InspectionDetailVO();
        BeanUtils.copyProperties(entity, vo);
        return vo;
    }

    private InspectionResultSummaryVO toResultVO(InspectionResultJpaEntity entity) {
        InspectionResultSummaryVO vo = new InspectionResultSummaryVO();
        BeanUtils.copyProperties(entity, vo);
        return vo;
    }
}
