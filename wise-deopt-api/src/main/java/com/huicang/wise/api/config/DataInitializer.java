package com.huicang.wise.api.config;

import com.huicang.wise.domain.alert.AlertEvent;
import com.huicang.wise.domain.auth.Permission;
import com.huicang.wise.domain.auth.Role;
import com.huicang.wise.domain.auth.RolePermission;
import com.huicang.wise.domain.auth.UserRole;
import com.huicang.wise.domain.auth.port.PasswordHasher;
import com.huicang.wise.domain.inventory.Product;
import com.huicang.wise.domain.repository.alert.AlertEventRepository;
import com.huicang.wise.domain.repository.auth.PermissionRepository;
import com.huicang.wise.domain.repository.auth.RolePermissionRepository;
import com.huicang.wise.domain.repository.auth.RoleRepository;
import com.huicang.wise.domain.repository.auth.UserRoleRepository;
import com.huicang.wise.domain.repository.inventory.ProductRepository;
import com.huicang.wise.domain.repository.tag.TagRepository;
import com.huicang.wise.domain.repository.user.NfcBadgeRepository;
import com.huicang.wise.domain.repository.user.UserCoreRepository;
import com.huicang.wise.domain.repository.user.UserProfileRepository;
import com.huicang.wise.domain.repository.user.UserSecurityRepository;
import com.huicang.wise.domain.repository.warehouse.WarehouseRepository;
import com.huicang.wise.domain.tag.ProductTag;
import com.huicang.wise.domain.user.NfcBadge;
import com.huicang.wise.domain.user.UserCore;
import com.huicang.wise.domain.user.UserProfile;
import com.huicang.wise.domain.user.UserSecurity;
import com.huicang.wise.domain.warehouse.Warehouse;
import java.time.LocalDateTime;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.annotation.Transactional;

@Configuration
public class DataInitializer {

    private static final Logger logger = LoggerFactory.getLogger(DataInitializer.class);

    @Value("${spring.auth.admin.username:admin}")
    private String adminUsername;

    @Value("${spring.auth.admin.password}")
    private String adminPassword;

    @Value("${spring.auth.admin.reset-password:false}")
    private boolean resetAdminPassword;

    @Value("${spring.auth.operator.username:operator}")
    private String operatorUsername;

    @Value("${spring.auth.operator.password}")
    private String operatorPassword;

    @Bean
    @Transactional
    public CommandLineRunner initData(
            UserCoreRepository userCoreRepository,
            UserSecurityRepository userSecurityRepository,
            UserProfileRepository userProfileRepository,
            NfcBadgeRepository nfcBadgeRepository,
            ProductRepository productRepository,
            WarehouseRepository warehouseRepository,
            TagRepository tagRepository,
            RoleRepository roleRepository,
            UserRoleRepository userRoleRepository,
            PermissionRepository permissionRepository,
            RolePermissionRepository rolePermissionRepository,
            AlertEventRepository alertEventRepository,
            PasswordHasher passwordHasher) {
        return args -> {
            logger.info("开始检查并初始化系统基础数据...");

            // 1. 初始化管理员用户
            if (!userCoreRepository.existsById(1L)) {
                logger.info("管理员用户不存在，开始创建...");

                // 创建 UserCore
                UserCore adminUser = new UserCore();
                adminUser.setUserId(1L);
                adminUser.setUsername(adminUsername);
                adminUser.setUserType((short) 0); // 普通用户类型
                adminUser.setStatus((short) 1); // 正常状态
                adminUser.setCreateTime(LocalDateTime.now());
                adminUser.setCreateBy(1L); // 自建
                adminUser.setUpdateTime(LocalDateTime.now());
                adminUser.setUpdateBy(1L);
                adminUser.setIsDeleted((short) 0);
                userCoreRepository.save(adminUser);

                // 创建 UserSecurity
                UserSecurity security = new UserSecurity();
                security.setUserId(1L);
                // 生成随机盐值
                String salt = UUID.randomUUID().toString().replace("-", "");
                security.setSalt(salt);
                security.setPasswordHash(passwordHasher.encode(adminPassword));
                security.setCreateTime(LocalDateTime.now());
                security.setUpdateTime(LocalDateTime.now());
                userSecurityRepository.save(security);

                // 创建 UserProfile
                UserProfile profile = new UserProfile();
                profile.setUserId(1L);
                profile.setNickname("系统管理员");
                profile.setEmail("admin@wisedepot.com");
                profile.setGender(1);
                profile.setCreateTime(LocalDateTime.now());
                profile.setUpdateTime(LocalDateTime.now());
                profile.setUpdateBy(1L);
                userProfileRepository.save(profile);

                logger.info("管理员用户创建完成，用户名: {}", adminUsername);
            } else if (resetAdminPassword) {
                logger.info("管理员用户已存在，检测到重置密码配置，开始重置密码...");
                UserSecurity security =
                        userSecurityRepository.findByUserId(1L).orElse(new UserSecurity());
                if (security.getUserId() == null) {
                    security.setUserId(1L);
                    String salt = UUID.randomUUID().toString().replace("-", "");
                    security.setSalt(salt);
                    security.setCreateTime(LocalDateTime.now());
                }
                security.setPasswordHash(passwordHasher.encode(adminPassword));
                security.setUpdateTime(LocalDateTime.now());
                userSecurityRepository.save(security);
                logger.info("管理员密码已重置");
            } else {
                logger.info("管理员用户已存在，且未配置重置密码，跳过密码更新");
            }

            if (nfcBadgeRepository.findByUserId(1L).isEmpty()) {
                logger.info("管理员NFC工牌不存在，开始创建...");

                try {
                    NfcBadge nfcBadge = new NfcBadge();
                    nfcBadge.setUserId(1L);
                    nfcBadge.setNfcUid("A3:C8:A1:21");
                    nfcBadge.setRfid(
                            "RFID_"
                                    + UUID.randomUUID()
                                            .toString()
                                            .replace("-", "")
                                            .substring(0, 16));
                    nfcBadge.setPinSalt("c892ef7493b841f9");
                    nfcBadge.setPinHash(
                            "$2a$12$3sHM6gdJX20291Q/JyfxGuC.wSO8.IjFjmM3ybfL6ddiROr407z2q");
                    nfcBadge.setStatus((short) 1);
                    nfcBadge.setCreateTime(LocalDateTime.now());
                    nfcBadge.setCreateBy(1L);
                    nfcBadge.setUpdateTime(LocalDateTime.now());
                    nfcBadge.setUpdateBy(1L);
                    NfcBadge saved = nfcBadgeRepository.save(nfcBadge);
                    logger.info(
                            "管理员NFC工牌创建完成，NFC UID: A3:C8:A1:21, Badge ID: {}", saved.getBadgeId());
                } catch (Exception e) {
                    logger.error("创建管理员NFC工牌失败", e);
                    throw e;
                }
            } else {
                logger.info("管理员NFC工牌已存在，跳过创建");
            }

            // 1.1 初始化普通用户（操作员）
            if (!userCoreRepository.existsById(2L)) {
                logger.info("普通用户不存在，开始创建...");

                UserCore operatorUser = new UserCore();
                operatorUser.setUserId(2L);
                operatorUser.setUsername(operatorUsername);
                operatorUser.setUserType((short) 0);
                operatorUser.setStatus((short) 1);
                operatorUser.setCreateTime(LocalDateTime.now());
                operatorUser.setCreateBy(1L);
                operatorUser.setUpdateTime(LocalDateTime.now());
                operatorUser.setUpdateBy(1L);
                operatorUser.setIsDeleted((short) 0);
                userCoreRepository.save(operatorUser);

                UserSecurity operatorSecurity = new UserSecurity();
                operatorSecurity.setUserId(2L);
                String operatorSalt = UUID.randomUUID().toString().replace("-", "");
                operatorSecurity.setSalt(operatorSalt);
                operatorSecurity.setPasswordHash(passwordHasher.encode(operatorPassword));
                operatorSecurity.setCreateTime(LocalDateTime.now());
                operatorSecurity.setUpdateTime(LocalDateTime.now());
                userSecurityRepository.save(operatorSecurity);

                UserProfile operatorProfile = new UserProfile();
                operatorProfile.setUserId(2L);
                operatorProfile.setNickname("普通操作员");
                operatorProfile.setEmail("operator@wisedepot.com");
                operatorProfile.setGender(1);
                operatorProfile.setCreateTime(LocalDateTime.now());
                operatorProfile.setUpdateTime(LocalDateTime.now());
                operatorProfile.setUpdateBy(1L);
                userProfileRepository.save(operatorProfile);

                logger.info("普通用户创建完成，用户名: {}", operatorUsername);
            } else {
                logger.info("普通用户已存在，跳过创建");
            }

            if (nfcBadgeRepository.findByUserId(2L).isEmpty()) {
                logger.info("普通用户NFC工牌不存在，开始创建...");

                try {
                    NfcBadge nfcBadge = new NfcBadge();
                    nfcBadge.setUserId(2L);
                    nfcBadge.setNfcUid("A3:C8:A1:22");
                    nfcBadge.setRfid(
                            "RFID_"
                                    + UUID.randomUUID()
                                            .toString()
                                            .replace("-", "")
                                            .substring(0, 16));
                    nfcBadge.setPinSalt("c892ef7493b841f9");
                    nfcBadge.setPinHash(
                            "$2a$12$3sHM6gdJX20291Q/JyfxGuC.wSO8.IjFjmM3ybfL6ddiROr407z2q");
                    nfcBadge.setStatus((short) 1);
                    nfcBadge.setCreateTime(LocalDateTime.now());
                    nfcBadge.setCreateBy(1L);
                    nfcBadge.setUpdateTime(LocalDateTime.now());
                    nfcBadge.setUpdateBy(1L);
                    NfcBadge saved = nfcBadgeRepository.save(nfcBadge);
                    logger.info(
                            "普通用户NFC工牌创建完成，NFC UID: A3:C8:A1:22, Badge ID: {}", saved.getBadgeId());
                } catch (Exception e) {
                    logger.error("创建普通用户NFC工牌失败", e);
                    throw e;
                }
            } else {
                logger.info("普通用户NFC工牌已存在，跳过创建");
            }

            // 2. 初始化角色
            createRoleIfNotExist(roleRepository, 1L, "管理员", "系统管理员，拥有所有权限");
            createRoleIfNotExist(roleRepository, 2L, "操作员", "普通操作员");
            createRoleIfNotExist(roleRepository, 3L, "访客", "访客");

            // 3. 初始化权限
            createPermissionIfNotExist(permissionRepository, 1L, "用户查看", "user:view");
            createPermissionIfNotExist(permissionRepository, 2L, "用户创建", "user:create");
            createPermissionIfNotExist(permissionRepository, 3L, "用户编辑", "user:edit");
            createPermissionIfNotExist(permissionRepository, 4L, "用户删除", "user:delete");

            // 4. 关联用户和角色
            if (userRoleRepository.findByUserId(1L).isEmpty()) {
                logger.info("管理员角色关联不存在，开始创建...");
                UserRole userRole = new UserRole();
                userRole.setUserId(1L);
                userRole.setRoleId(1L); // ADMIN
                userRole.setCreateTime(LocalDateTime.now());
                userRole.setCreateBy(1L);
                userRoleRepository.save(userRole);
                logger.info("管理员角色关联创建完成");
            }

            if (userRoleRepository.findByUserId(2L).isEmpty()) {
                logger.info("普通用户角色关联不存在，开始创建...");
                UserRole userRole = new UserRole();
                userRole.setUserId(2L);
                userRole.setRoleId(2L); // OPERATOR
                userRole.setCreateTime(LocalDateTime.now());
                userRole.setCreateBy(1L);
                userRoleRepository.save(userRole);
                logger.info("普通用户角色关联创建完成");
            }

            // 5. 关联角色和权限（管理员拥有所有权限）
            createRolePermissionIfNotExist(rolePermissionRepository, 1L, 1L);
            createRolePermissionIfNotExist(rolePermissionRepository, 1L, 2L);
            createRolePermissionIfNotExist(rolePermissionRepository, 1L, 3L);
            createRolePermissionIfNotExist(rolePermissionRepository, 1L, 4L);

            // 6. 初始化测试仓库、产品和标签
            initTestWarehouseProductsAndTags(warehouseRepository, productRepository, tagRepository);

            // 7. 初始化告警假数据
            initAlertData(alertEventRepository);

            logger.info("系统基础数据初始化完成");
        };
    }

    private void initTestWarehouseProductsAndTags(
            WarehouseRepository warehouseRepository,
            ProductRepository productRepository,
            TagRepository tagRepository) {
        logger.info("开始初始化测试仓库、产品和标签...");

        try {
            // 创建测试仓库
            Long warehouseId = createWarehouseIfNotExist(warehouseRepository, 1L, "测试仓库1", "WH001");

            // 创建产品和标签数据
            createProductAndTagIfNotExist(
                    productRepository,
                    tagRepository,
                    warehouseId,
                    1L,
                    "视频分割器",
                    "E28278020000000029D0FD6D",
                    "6950629140189");
            createProductAndTagIfNotExist(
                    productRepository,
                    tagRepository,
                    warehouseId,
                    2L,
                    "水晶头",
                    "E28278020000000029D0DFA6",
                    "6970583880105");
            createProductAndTagIfNotExist(
                    productRepository,
                    tagRepository,
                    warehouseId,
                    3L,
                    "路由器",
                    "E28068940000503287D66041",
                    "8252674081300");
            createProductAndTagIfNotExist(
                    productRepository,
                    tagRepository,
                    warehouseId,
                    4L,
                    "扫描器",
                    "E28278020000000029D0DF96",
                    "6973138764646");
            createProductAndTagIfNotExist(
                    productRepository,
                    tagRepository,
                    warehouseId,
                    5L,
                    "无线AP1",
                    "E28068940000403287D66441",
                    "6921168509256");
            createProductAndTagIfNotExist(
                    productRepository,
                    tagRepository,
                    warehouseId,
                    6L,
                    "无线AP2",
                    "E28278020000000029D0FD5D",
                    "6921168509257");
            createProductAndTagIfNotExist(
                    productRepository,
                    tagRepository,
                    warehouseId,
                    7L,
                    "显示器",
                    "E28278020000000029D17803",
                    "6976570310457");

            logger.info("测试仓库、产品和标签初始化完成");
        } catch (Exception e) {
            logger.error("初始化测试仓库、产品和标签失败", e);
            throw e;
        }
    }

    private Long createWarehouseIfNotExist(
            WarehouseRepository warehouseRepository, Long warehouseId, String name, String code) {
        if (!warehouseRepository.existsById(warehouseId)) {
            Warehouse warehouse = new Warehouse();
            warehouse.setWarehouseId(warehouseId);
            warehouse.setWarehouseName(name);
            warehouse.setWarehouseCode(code);
            warehouse.setDescription("系统初始化创建的测试仓库");
            warehouse.setCreateTime(LocalDateTime.now());
            warehouse.setUpdateTime(LocalDateTime.now());
            warehouseRepository.save(warehouse);
            logger.info("仓库 {} 创建完成", name);
        }
        return warehouseId;
    }

    private void createProductAndTagIfNotExist(
            ProductRepository productRepository,
            TagRepository tagRepository,
            Long warehouseId,
            Long productId,
            String productName,
            String rfid,
            String barcode) {
        try {
            if (!productRepository.existsById(productId)) {
                Product product = new Product();
                product.setProductId(productId);
                product.setName(productName);
                product.setCode("PRD" + String.format("%03d", productId));
                product.setModel("标准型号");
                product.setUnit("个");
                product.setCreateBy(1L);
                product.setCreateTime(LocalDateTime.now());
                product.setUpdateBy(1L);
                product.setUpdateTime(LocalDateTime.now());
                productRepository.save(product);
                logger.info("产品 {} 创建完成", productName);
            }

            if (!tagRepository.existsByRfid(rfid)) {
                ProductTag tag = new ProductTag();
                tag.setProductId(productId);
                tag.setRfid(rfid);
                tag.setBarcode(barcode);
                tag.setNfcUid("NFC_" + rfid.substring(rfid.length() - 8));
                tag.setStatus((short) 1);
                tag.setCreateBy(1L);
                tag.setCreateTime(LocalDateTime.now());
                tag.setUpdateTime(LocalDateTime.now());
                tagRepository.save(tag);
                logger.info("标签 {} 创建完成并绑定到产品 {}", rfid, productName);
            }
        } catch (Exception e) {
            logger.error("创建产品 {} 或标签 {} 失败", productName, rfid, e);
            throw e;
        }
    }

    private void createRoleIfNotExist(
            RoleRepository roleRepository, Long roleId, String name, String description) {
        if (!roleRepository.existsById(roleId)) {
            Role role = new Role();
            role.setRoleId(roleId);
            role.setName(name);
            role.setDescription(description);
            role.setCreateTime(LocalDateTime.now());
            role.setCreateBy(1L);
            role.setUpdateTime(LocalDateTime.now());
            role.setUpdateBy(1L);
            roleRepository.save(role);
            logger.info("角色 {} 创建完成", name);
        }
    }

    private void createPermissionIfNotExist(
            PermissionRepository permissionRepository,
            Long permissionId,
            String name,
            String code) {
        if (permissionRepository.findById(permissionId).isEmpty()) {
            Permission permission = new Permission();
            permission.setPermissionId(permissionId);
            permission.setName(name);
            permission.setCode(code);
            permission.setDescription(name);
            permission.setCreateTime(LocalDateTime.now());
            permission.setCreateBy(1L);
            permission.setUpdateTime(LocalDateTime.now());
            permission.setUpdateBy(1L);
            permissionRepository.save(permission);
            logger.info("权限 {} ({}) 创建完成", name, code);
        }
    }

    private void createRolePermissionIfNotExist(
            RolePermissionRepository rolePermissionRepository, Long roleId, Long permissionId) {
        if (rolePermissionRepository.findByRoleId(roleId).stream()
                .noneMatch(rp -> rp.getPermissionId().equals(permissionId))) {
            RolePermission rolePermission = new RolePermission();
            rolePermission.setRoleId(roleId);
            rolePermission.setPermissionId(permissionId);
            rolePermission.setCreateTime(LocalDateTime.now());
            rolePermission.setCreateBy(1L);
            rolePermissionRepository.save(rolePermission);
            logger.info("角色 {} 与权限 {} 关联创建完成", roleId, permissionId);
        }
    }

    private void initAlertData(AlertEventRepository alertEventRepository) {
        logger.info("开始初始化告警假数据...");

        try {
            if (alertEventRepository.count() == 0) {
                logger.info("告警表为空，开始创建告警假数据...");

                AlertEvent alert1 = new AlertEvent();
                alert1.setSourceModule("RFID");
                alert1.setLevel((short) 3);
                alert1.setTitle("违规移动告警");
                alert1.setMessage(
                        "检测到违规移动！设备：RFID固定读写器(RFID-10-0-0-70)，RFID：E28278020000000029D0FD6D，产品ID：1");
                alert1.setStatus((short) 0);
                alert1.setIsActive(true);
                alert1.setCreateTime(LocalDateTime.now().minusHours(1));
                alertEventRepository.save(alert1);
                logger.info("告警1创建完成: 违规移动告警");

                logger.info("告警假数据初始化完成，共创建 {} 条告警", 5);
            } else {
                logger.info("告警数据已存在，跳过初始化");
            }
        } catch (Exception e) {
            logger.error("初始化告警假数据失败", e);
            throw e;
        }
    }
}
