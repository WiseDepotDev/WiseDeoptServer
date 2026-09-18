package com.huicang.wise.api.support;

import com.huicang.wise.application.accesskey.AccessKeyApplicationService;
import com.huicang.wise.application.alert.AlertApplicationService;
import com.huicang.wise.application.alert.AlertRuleService;
import com.huicang.wise.application.captcha.CaptchaApplicationService;
import com.huicang.wise.application.dashboard.DashboardApplicationService;
import com.huicang.wise.application.device.DeviceApplicationService;
import com.huicang.wise.application.i18n.I18nService;
import com.huicang.wise.application.inout.InOutApplicationService;
import com.huicang.wise.application.inspection.InspectionApplicationService;
import com.huicang.wise.application.inventory.InventoryApplicationService;
import com.huicang.wise.application.message.MessageApplicationService;
import com.huicang.wise.application.oss.FileStorageApplicationService;
import com.huicang.wise.application.oss.OssApplicationService;
import com.huicang.wise.application.password.PasswordApplicationService;
import com.huicang.wise.application.permission.PermissionApplicationService;
import com.huicang.wise.application.permission.PermissionService;
import com.huicang.wise.application.report.ReportApplicationService;
import com.huicang.wise.application.rfid.RfidDataApplicationService;
import com.huicang.wise.application.role.RoleApplicationService;
import com.huicang.wise.application.sync.SyncApplicationService;
import com.huicang.wise.application.tag.TagApplicationService;
import com.huicang.wise.application.user.UserProfileApplicationService;
import com.huicang.wise.application.user.UserRoleApplicationService;
import com.huicang.wise.application.warehouse.WarehouseApplicationService;
import com.huicang.wise.infrastructure.persistence.repository.oss.MinioFileRepository;
import com.huicang.wise.infrastructure.persistence.repository.user.UserAccessKeyRepository;
import com.huicang.wise.infrastructure.security.JwtTokenProvider;
import com.huicang.wise.infrastructure.security.RateLimiterService;
import org.springframework.boot.test.mock.mockito.MockBean;

/**
 * Web 切片测试（{@code @WebMvcTest}）的公共桩件基类。
 *
 * <p>背景：{@code WiseDeoptServerApplication} 声明了 {@code scanBasePackages = "com.huicang.wise"}， 导致
 * {@code @WebMvcTest} 切片实际会加载跨模块组件；这些组件依赖 application / domain / infrastructure 模块的 bean，切片内不存在，从而
 * {@code Failed to load ApplicationContext}。
 *
 * <p>实现说明：必须使用 {@code @MockBean}（由 Spring Boot 注册为 mock 定义）， <b>不能</b>在 {@code @TestConfiguration}
 * 中用 {@code @Bean} 返回 {@code Mockito.mock(...)}—— 后者仍会被 Spring 的字段注入后处理器处理，从而反向要求缺失的依赖，问题不会消失。
 *
 * <p>用法：测试类 {@code extends AbstractWebMvcSliceTest}； 需要对其调用打桩时直接使用本类中的 protected 字段。
 *
 * @author WiseDepot
 * @version 1.0
 * @since 2026-02-27
 */
public abstract class AbstractWebMvcSliceTest {

    @MockBean protected AccessKeyApplicationService accessKeyApplicationService;

    @MockBean protected AlertApplicationService alertApplicationService;

    @MockBean protected AlertRuleService alertRuleService;

    @MockBean protected CaptchaApplicationService captchaApplicationService;

    @MockBean protected DashboardApplicationService dashboardApplicationService;

    @MockBean protected DeviceApplicationService deviceApplicationService;

    @MockBean protected I18nService i18nService;

    @MockBean protected SyncApplicationService syncApplicationService;

    @MockBean protected MessageApplicationService messageApplicationService;

    @MockBean protected InspectionApplicationService inspectionApplicationService;

    @MockBean protected InOutApplicationService inOutApplicationService;

    @MockBean protected InventoryApplicationService inventoryApplicationService;

    @MockBean protected FileStorageApplicationService fileStorageApplicationService;

    @MockBean protected OssApplicationService ossApplicationService;

    @MockBean protected PasswordApplicationService passwordApplicationService;

    @MockBean protected PermissionApplicationService permissionApplicationService;

    @MockBean protected PermissionService permissionService;

    @MockBean protected ReportApplicationService reportApplicationService;

    @MockBean protected RfidDataApplicationService rfidDataApplicationService;

    @MockBean protected RoleApplicationService roleApplicationService;

    @MockBean protected TagApplicationService tagApplicationService;

    @MockBean protected UserProfileApplicationService userProfileApplicationService;

    @MockBean protected UserRoleApplicationService userRoleApplicationService;

    @MockBean protected WarehouseApplicationService warehouseApplicationService;

    @MockBean protected MinioFileRepository minioFileRepository;

    @MockBean protected UserAccessKeyRepository userAccessKeyRepository;

    @MockBean protected JwtTokenProvider jwtTokenProvider;

    @MockBean protected RateLimiterService rateLimiterService;
}
