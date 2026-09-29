package com.huicang.wise.application.permission;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.huicang.wise.common.api.ErrorCode;
import com.huicang.wise.common.exception.BusinessException;
import com.huicang.wise.domain.auth.Permission;
import com.huicang.wise.infrastructure.persistence.repository.auth.PermissionRepository;
import java.lang.reflect.Method;
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
 * 权限应用服务的单元测试：查询、增改删，以及权限树构建。
 *
 * <p>本批钉住四点现状（只记录、未修）： ① 树构建**没有环检测**，且父节点不存在的"孤儿"节点会被**静默丢弃**（既不是根、也不挂到任何父下）； ② 叶子节点**不会被设置
 * children**（只有非空才 set），因此叶子处 <code>getChildren()</code> 为 null； ③ 类里有两个**恒返回 false 的私有桩方法**（<code>
 * hasChildren</code> / <code>hasCircularDependency</code>）**从未被调用**； ④ <code>updatePermission
 * </code> **不会改权限编码**（只改名称/描述），而创建时编码唯一性有校验。
 */
@ExtendWith(MockitoExtension.class)
class PermissionApplicationServiceTest {

    @Mock private PermissionRepository permissionRepository;
    @Mock private PermissionMapper permissionMapper;

    private PermissionApplicationService service;

    @BeforeEach
    void setUp() {
        service = new PermissionApplicationService(permissionRepository, permissionMapper);
    }

    private Permission permission(Long id, Long parentId, String code) {
        Permission entity = new Permission();
        entity.setPermissionId(id);
        // 注：Permission 实体没有 parentId 字段，分层信息只存在于 DTO 侧
        entity.setCode(code);
        entity.setName("名称" + id);
        entity.setDescription("描述" + id);
        return entity;
    }

    private PermissionDTO dto(Long id, Long parentId) {
        PermissionDTO d = new PermissionDTO();
        d.setPermissionId(id);
        d.setParentId(parentId);
        return d;
    }

    // ---------------- 查询 ----------------

    @Test
    @DisplayName("全部权限：逐条映射")
    void getAllPermissionsMapsAll() {
        when(permissionRepository.findAll()).thenReturn(List.of(permission(1L, null, "a")));
        when(permissionMapper.toDTO(any(Permission.class))).thenReturn(dto(1L, null));

        assertEquals(1, service.getAllPermissions().size());
    }

    @Test
    @DisplayName("按ID查：不存在抛 NOT_FOUND")
    void getByIdMissing() {
        when(permissionRepository.findById(9L)).thenReturn(Optional.empty());

        BusinessException ex =
                assertThrows(BusinessException.class, () -> service.getPermissionById(9L));

        assertEquals(ErrorCode.NOT_FOUND, ex.getErrorCode());
    }

    @Test
    @DisplayName("按ID查：命中并映射")
    void getByIdFound() {
        Permission entity = permission(1L, null, "a");
        when(permissionRepository.findById(1L)).thenReturn(Optional.of(entity));
        when(permissionMapper.toDTO(entity)).thenReturn(dto(1L, null));

        assertNotNull(service.getPermissionById(1L));
    }

    @Test
    @DisplayName("按编码查：不存在抛 NOT_FOUND；命中返回")
    void getByCode() {
        when(permissionRepository.findByCode("nope")).thenReturn(Optional.empty());
        BusinessException ex =
                assertThrows(BusinessException.class, () -> service.getPermissionByCode("nope"));
        assertEquals(ErrorCode.NOT_FOUND, ex.getErrorCode());

        Permission entity = permission(2L, null, "user:read");
        when(permissionRepository.findByCode("user:read")).thenReturn(Optional.of(entity));
        when(permissionMapper.toDTO(entity)).thenReturn(dto(2L, null));
        assertNotNull(service.getPermissionByCode("user:read"));
    }

    // ---------------- 权限树 ----------------

    @Test
    @DisplayName("权限树：只有 parentId 为 null 的才是根")
    void treeRootsAreTopLevelOnly() {
        Permission root = permission(1L, null, "a");
        Permission child = permission(2L, 1L, "b");
        when(permissionRepository.findAll()).thenReturn(List.of(root, child));
        when(permissionMapper.toDTO(root)).thenReturn(dto(1L, null));
        when(permissionMapper.toDTO(child)).thenReturn(dto(2L, 1L));

        List<PermissionDTO> tree = service.getPermissionTree();

        assertEquals(1, tree.size());
        assertEquals(1L, tree.get(0).getPermissionId().longValue());
        assertEquals(1, tree.get(0).getChildren().size());
        assertEquals(2L, tree.get(0).getChildren().get(0).getPermissionId().longValue());
    }

    @Test
    @DisplayName("权限树：支持多级嵌套（孙节点也会挂上）")
    void treeSupportsNestedLevels() {
        Permission root = permission(1L, null, "a");
        Permission mid = permission(2L, 1L, "b");
        Permission leaf = permission(3L, 2L, "c");
        when(permissionRepository.findAll()).thenReturn(List.of(root, mid, leaf));
        when(permissionMapper.toDTO(root)).thenReturn(dto(1L, null));
        when(permissionMapper.toDTO(mid)).thenReturn(dto(2L, 1L));
        when(permissionMapper.toDTO(leaf)).thenReturn(dto(3L, 2L));

        PermissionDTO tree = service.getPermissionTree().get(0);

        assertEquals(2L, tree.getChildren().get(0).getPermissionId().longValue());
        assertEquals(
                3L, tree.getChildren().get(0).getChildren().get(0).getPermissionId().longValue());
    }

    @Test
    @DisplayName("权限树：叶子节点不会被设置 children（保持 null）")
    void leafNodesHaveNoChildrenList() {
        Permission root = permission(1L, null, "a");
        Permission leaf = permission(2L, 1L, "b");
        when(permissionRepository.findAll()).thenReturn(List.of(root, leaf));
        when(permissionMapper.toDTO(root)).thenReturn(dto(1L, null));
        when(permissionMapper.toDTO(leaf)).thenReturn(dto(2L, 1L));

        PermissionDTO tree = service.getPermissionTree().get(0);

        assertNull(tree.getChildren().get(0).getChildren(), "现状：只有非空 children 才会被 set，叶子节点保持 null");
    }

    @Test
    @DisplayName("现状：父节点不存在的孤儿权限会被静默丢弃（既不呈现也不报错）")
    void orphanNodesAreSilentlyDropped() {
        Permission root = permission(1L, null, "a");
        Permission orphan = permission(9L, 999L, "orphan");
        when(permissionRepository.findAll()).thenReturn(List.of(root, orphan));
        when(permissionMapper.toDTO(root)).thenReturn(dto(1L, null));
        when(permissionMapper.toDTO(orphan)).thenReturn(dto(9L, 999L));

        List<PermissionDTO> tree = service.getPermissionTree();

        assertEquals(1, tree.size(), "现状：孤儿节点不会出现在树里");
        assertEquals(1L, tree.get(0).getPermissionId().longValue());
        assertTrue(tree.get(0).getChildren() == null, "现状：没有子节点时不会设置 children");
    }

    @Test
    @DisplayName("权限树：没有任何权限时返回空表")
    void treeWithNoPermissions() {
        when(permissionRepository.findAll()).thenReturn(List.of());

        assertTrue(service.getPermissionTree().isEmpty());
    }

    // ---------------- 增改删 ----------------

    @Test
    @DisplayName("创建权限：编码重复抛 VAL_CONFLICT_PERMISSION_CODE_EXISTS 且不落库")
    void createRejectsDuplicateCode() {
        CreatePermissionRequest request = new CreatePermissionRequest();
        request.setPermissionCode("user:read");
        when(permissionRepository.findByCode("user:read"))
                .thenReturn(Optional.of(permission(1L, null, "user:read")));

        BusinessException ex =
                assertThrows(BusinessException.class, () -> service.createPermission(request));

        assertEquals(ErrorCode.VAL_CONFLICT_PERMISSION_CODE_EXISTS, ex.getErrorCode());
        verify(permissionRepository, never()).save(any(Permission.class));
    }

    @Test
    @DisplayName("创建权限：写入名称/编码/描述与两个时间戳")
    void createFillsFields() {
        CreatePermissionRequest request = new CreatePermissionRequest();
        request.setPermissionCode("user:read");
        request.setPermissionName("查看用户");
        request.setDescription("只读");
        when(permissionRepository.findByCode("user:read")).thenReturn(Optional.empty());
        when(permissionRepository.save(any(Permission.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(permissionMapper.toDTO(any(Permission.class))).thenReturn(dto(1L, null));

        service.createPermission(request);

        ArgumentCaptor<Permission> captor = ArgumentCaptor.forClass(Permission.class);
        verify(permissionRepository).save(captor.capture());
        Permission saved = captor.getValue();
        assertEquals("user:read", saved.getCode());
        assertEquals("查看用户", saved.getName());
        assertEquals("只读", saved.getDescription());
        assertNotNull(saved.getCreateTime());
        assertNotNull(saved.getUpdateTime());
    }

    @Test
    @DisplayName("更新权限：不存在抛 NOT_FOUND")
    void updateMissing() {
        when(permissionRepository.findById(9L)).thenReturn(Optional.empty());

        BusinessException ex =
                assertThrows(
                        BusinessException.class,
                        () -> service.updatePermission(9L, new UpdatePermissionRequest()));

        assertEquals(ErrorCode.NOT_FOUND, ex.getErrorCode());
    }

    @Test
    @DisplayName("更新权限：只改名称/描述与更新时间，**编码不动**（现状）")
    void updateKeepsCode() {
        Permission entity = permission(1L, null, "user:read");
        when(permissionRepository.findById(1L)).thenReturn(Optional.of(entity));
        when(permissionRepository.save(any(Permission.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(permissionMapper.toDTO(any(Permission.class))).thenReturn(dto(1L, null));
        UpdatePermissionRequest request = new UpdatePermissionRequest();
        request.setPermissionName("新名称");
        request.setDescription("新描述");

        service.updatePermission(1L, request);

        assertEquals("新名称", entity.getName());
        assertEquals("新描述", entity.getDescription());
        assertEquals("user:read", entity.getCode(), "现状：更新请求里没有编码，也不会改编码");
        assertNotNull(entity.getUpdateTime());
    }

    @Test
    @DisplayName("删除权限：不存在抛 NOT_FOUND 且不删除")
    void deleteMissing() {
        when(permissionRepository.findById(9L)).thenReturn(Optional.empty());

        BusinessException ex =
                assertThrows(BusinessException.class, () -> service.deletePermission(9L));

        assertEquals(ErrorCode.NOT_FOUND, ex.getErrorCode());
        verify(permissionRepository, never()).delete(any(Permission.class));
    }

    @Test
    @DisplayName("删除权限：存在则删除实体")
    void deleteSuccess() {
        Permission entity = permission(1L, null, "a");
        when(permissionRepository.findById(1L)).thenReturn(Optional.of(entity));

        service.deletePermission(1L);

        verify(permissionRepository).delete(entity);
    }

    // ---------------- 死代码固定 ----------------

    @Test
    @DisplayName("现状：两个私有桩方法恒返回 false，且类内无人调用（死代码）")
    void stubMethodsAlwaysReturnFalse() throws Exception {
        Method hasChildren =
                PermissionApplicationService.class.getDeclaredMethod("hasChildren", Long.class);
        Method hasCircular =
                PermissionApplicationService.class.getDeclaredMethod(
                        "hasCircularDependency", Long.class, Long.class);
        hasChildren.setAccessible(true);
        hasCircular.setAccessible(true);

        assertEquals(Boolean.FALSE, hasChildren.invoke(service, 1L));
        assertEquals(Boolean.FALSE, hasCircular.invoke(service, 1L, 2L));
    }

    @Test
    @DisplayName("权限树构建不改写入参列表顺序（按仓库返回顺序拼接）")
    void treePreservesRepositoryOrder() {
        Permission a = permission(1L, null, "a");
        Permission b = permission(2L, null, "b");
        when(permissionRepository.findAll()).thenReturn(List.of(b, a));
        when(permissionMapper.toDTO(a)).thenReturn(dto(1L, null));
        when(permissionMapper.toDTO(b)).thenReturn(dto(2L, null));

        List<PermissionDTO> tree = service.getPermissionTree();

        assertEquals(2L, tree.get(0).getPermissionId().longValue());
        assertEquals(1L, tree.get(1).getPermissionId().longValue());
    }

    @Test
    @DisplayName("创建权限：不传描述时描述为 null（不编造）")
    void createAllowsNullDescription() {
        CreatePermissionRequest request = new CreatePermissionRequest();
        request.setPermissionCode("x");
        when(permissionRepository.findByCode("x")).thenReturn(Optional.empty());
        when(permissionRepository.save(any(Permission.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(permissionMapper.toDTO(any(Permission.class))).thenReturn(dto(1L, null));

        service.createPermission(request);

        ArgumentCaptor<Permission> captor = ArgumentCaptor.forClass(Permission.class);
        verify(permissionRepository).save(captor.capture());
        assertNull(captor.getValue().getDescription());
        assertNotNull(captor.getValue().getCreateTime());
        assertEquals(LocalDateTime.class, captor.getValue().getCreateTime().getClass());
    }

    @Test
    @DisplayName("现状：Permission 实体没有 parentId setter —— 分层只可能来自 DTO 侧")
    void entityHasNoParentId() {
        assertThrows(
                NoSuchMethodException.class,
                () -> Permission.class.getDeclaredMethod("setParentId", Long.class),
                "现状：实体没有 parentId，故映射器无法从实体填充 DTO 的 parentId；" + "权限树的分层信息实际缺少实体侧来源（需确认是否有别的填充点）");
    }
}
