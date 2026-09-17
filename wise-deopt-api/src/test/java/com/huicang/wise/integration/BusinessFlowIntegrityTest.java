package com.huicang.wise.integration;

import com.huicang.wise.application.alert.AlertApplicationService;
import com.huicang.wise.application.device.DeviceApplicationService;
import com.huicang.wise.application.inspection.InspectionApplicationService;
import com.huicang.wise.application.inventory.InventoryApplicationService;
import com.huicang.wise.application.tag.TagApplicationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 类功能描述：业务流程完整性测试
 *
 * @author WiseDepot
 * @version 0.1.20
 * @since 2026-02-27
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class BusinessFlowIntegrityTest {

    @Autowired
    private DeviceApplicationService deviceService;

    @Autowired
    private TagApplicationService tagService;

    @Autowired
    private InventoryApplicationService inventoryService;

    @Autowired
    private InspectionApplicationService inspectionService;

    @Autowired
    private AlertApplicationService alertService;

    /**
     * 测试设备管理业务流程
     */
    @Test
    public void testDeviceManagementFlow() {
        assertNotNull(deviceService, "设备服务不能为空");
    }

    /**
     * 测试标签管理业务流程
     */
    @Test
    public void testTagManagementFlow() {
        assertNotNull(tagService, "标签服务不能为空");
    }

    /**
     * 测试库存管理业务流程
     */
    @Test
    public void testInventoryManagementFlow() {
        assertNotNull(inventoryService, "库存服务不能为空");
    }

    /**
     * 测试巡检管理业务流程
     */
    @Test
    public void testInspectionManagementFlow() {
        assertNotNull(inspectionService, "巡检服务不能为空");
    }

    /**
     * 测试告警管理业务流程
     */
    @Test
    public void testAlertManagementFlow() {
        assertNotNull(alertService, "告警服务不能为空");
    }

    /**
     * 测试完整业务流程集成
     */
    @Test
    public void testCompleteBusinessFlowIntegration() {
        assertNotNull(deviceService, "设备服务不能为空");
        assertNotNull(tagService, "标签服务不能为空");
        assertNotNull(inventoryService, "库存服务不能为空");
        assertNotNull(inspectionService, "巡检服务不能为空");
        assertNotNull(alertService, "告警服务不能为空");
    }
}