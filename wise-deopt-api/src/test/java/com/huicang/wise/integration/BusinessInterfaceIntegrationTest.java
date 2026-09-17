package com.huicang.wise.integration;

import com.huicang.wise.application.alert.AlertApplicationService;
import com.huicang.wise.application.auth.AuthApplicationService;
import com.huicang.wise.application.device.DeviceApplicationService;
import com.huicang.wise.application.inspection.InspectionApplicationService;
import com.huicang.wise.application.inventory.InventoryApplicationService;
import com.huicang.wise.application.oss.FileStorageApplicationService;
import com.huicang.wise.application.report.ReportApplicationService;
import com.huicang.wise.application.tag.TagApplicationService;
import com.huicang.wise.application.user.UserApplicationService;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 类功能描述：业务接口集成测试
 *
 * @author WiseDepot
 * @version 0.1.20
 * @since 2026-02-27
 */
@Tag("e2e")
@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class BusinessInterfaceIntegrationTest {

    @Autowired
    private AuthApplicationService authService;

    @Autowired
    private UserApplicationService userService;

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

    @Autowired
    private ReportApplicationService reportService;

    @Autowired
    private FileStorageApplicationService fileStorageService;

    /**
     * 测试认证服务接口
     */
    @Test
    public void testAuthServiceInterfaces() {
        assertNotNull(authService, "认证服务不能为空");
    }

    /**
     * 测试用户服务接口
     */
    @Test
    public void testUserServiceInterfaces() {
        assertNotNull(userService, "用户服务不能为空");
    }

    /**
     * 测试设备服务接口
     */
    @Test
    public void testDeviceServiceInterfaces() {
        assertNotNull(deviceService, "设备服务不能为空");
    }

    /**
     * 测试标签服务接口
     */
    @Test
    public void testTagServiceInterfaces() {
        assertNotNull(tagService, "标签服务不能为空");
    }

    /**
     * 测试库存服务接口
     */
    @Test
    public void testInventoryServiceInterfaces() {
        assertNotNull(inventoryService, "库存服务不能为空");
    }

    /**
     * 测试巡检服务接口
     */
    @Test
    public void testInspectionServiceInterfaces() {
        assertNotNull(inspectionService, "巡检服务不能为空");
    }

    /**
     * 测试告警服务接口
     */
    @Test
    public void testAlertServiceInterfaces() {
        assertNotNull(alertService, "告警服务不能为空");
    }

    /**
     * 测试报表服务接口
     */
    @Test
    public void testReportServiceInterfaces() {
        assertNotNull(reportService, "报表服务不能为空");
    }

    /**
     * 测试文件存储服务接口
     */
    @Test
    public void testFileStorageServiceInterfaces() {
        assertNotNull(fileStorageService, "文件存储服务不能为空");
    }
}