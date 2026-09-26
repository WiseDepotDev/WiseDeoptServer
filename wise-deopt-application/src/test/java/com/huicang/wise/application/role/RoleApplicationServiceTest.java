package com.huicang.wise.application.role;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.huicang.wise.application.permission.PermissionDTO;
import com.huicang.wise.application.permission.PermissionMapper;
import com.huicang.wise.common.api.ErrorCode;
import com.huicang.wise.common.exception.BusinessException;
import com.huicang.wise.domain.auth.Permission;
import com.huicang.wise.domain.auth.Role;
import com.huicang.wise.domain.auth.RolePermission;
import com.huicang.wise.infrastructure.persistence.repository.auth.PermissionRepository;
import com.huicang.wise.infrastructure.persistence.repository.auth.RolePermissionRepository;
import com.huicang.wise.infrastructure.persistence.repository.auth.RoleRepository;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * {@link RoleApplicationService} 单元测试（P2-11 应用层补测）。
 *
 * <p>应用层是本次重构风险最高的层（纯编排逻辑，既无框架兜底也最容易被改错），而它此前**行覆盖 0%**。
 * 本测试按"每个公开方法的两条路径"组织：**正常路径**与**失败路径**，并对**副作用顺序**做断言 （例如删除角色必须先清 `role_permission` 再删
 * `role`，否则会留下悬挂关联）。
 *
 * @author WiseDepot
 * @version 1.0
 * @since 2026-09-26
 */
@ExtendWith(MockitoExtension.class)
class RoleApplicationServiceTest {

    @Mock private RoleRepository roleRepository;
    @Mock private PermissionRepository permissionRepository;
    @Mock private RolePermissionRepository rolePermissionRepository;
    @Mock private RoleMapper roleMapper;
    @Mock private PermissionMapper permissionMapper;

    @InjectMocks private RoleApplicationService service;

    private static Role role(Long id, String name) {
        Role r = new Role();
        r.setRoleId(id);
        r.setName(name);
        r.setDescription("desc-" + name);
        r.setCreateTime(LocalDateTime.of(2026, 9, 1, 10, 0));
        r.setCreateBy(7L);
        r.setUpdateTime(LocalDateTime.of(2026, 9, 2, 11, 0));
        r.setUpdateBy(8L);
        return r;
    }

    private static Permission permission(Long id, String name) {
        Permission p = new Permission();
        p.setPermissionId(id);
        p.setName(name);
        return p;
    }

    private static RolePermission rolePermission(Long roleId, Long permissionId) {
        RolePermission rp = new RolePermission();
        rp.setRoleId(roleId);
        rp.setPermissionId(permissionId);
        return rp;
    }

    // ---------------------------------------------------------------- 查询

    @Test
    @DisplayName("getAllRoles：把仓储返回的每个角色都映射成 DTO，顺序保持")
    void getAllRolesShouldMapEveryRole() {
        when(roleRepository.findAll()).thenReturn(List.of(role(1L, "管理员"), role(2L, "操作员")));
        when(roleMapper.toDTO(any(Role.class)))
                .thenAnswer(
                        inv -> {
                            Role r = inv.getArgument(0);
                            RoleDTO dto = new RoleDTO();
                            dto.setRoleId(r.getRoleId());
                            dto.setName(r.getName());
                            return dto;
                        });

        List<RoleDTO> result = service.getAllRoles();

        assertEquals(2, result.size());
        assertEquals(1L, result.get(0).getRoleId());
        assertEquals("操作员", result.get(1).getName());
    }

    @Test
    @DisplayName("getRoleById：命中时把该角色的权限一并挂到 DTO 上")
    void getRoleByIdShouldAttachPermissions() {
        when(roleRepository.findById(1L)).thenReturn(Optional.of(role(1L, "管理员")));
        when(roleMapper.toDTO(any(Role.class))).thenReturn(new RoleDTO());
        when(rolePermissionRepository.findByRoleId(1L))
                .thenReturn(List.of(rolePermission(1L, 10L), rolePermission(1L, 20L)));
        when(permissionRepository.findAllById(List.of(10L, 20L)))
                .thenReturn(List.of(permission(10L, "读"), permission(20L, "写")));
        when(permissionMapper.toDTO(any(Permission.class)))
                .thenAnswer(
                        inv -> {
                            Permission p = inv.getArgument(0);
                            PermissionDTO dto = new PermissionDTO();
                            dto.setPermissionId(p.getPermissionId());
                            return dto;
                        });

        RoleDTO dto = service.getRoleById(1L);

        assertNotNull(dto);
        assertNotNull(dto.getPermissions());
        assertEquals(2, dto.getPermissions().size());
        assertEquals(10L, dto.getPermissions().get(0).getPermissionId());
    }

    @Test
    @DisplayName("getRoleById：ID 不存在时抛 BusinessException(NOT_FOUND)")
    void getRoleByIdWhenMissingShouldThrowNotFound() {
        when(roleRepository.findById(99L)).thenReturn(Optional.empty());

        BusinessException ex =
                assertThrows(BusinessException.class, () -> service.getRoleById(99L));

        assertEquals(ErrorCode.NOT_FOUND, ex.getErrorCode());
    }

    @Test
    @DisplayName("getRoleByName：命中时挂权限")
    void getRoleByNameShouldAttachPermissions() {
        when(roleRepository.findByName("管理员")).thenReturn(Optional.of(role(1L, "管理员")));
        when(roleMapper.toDTO(any(Role.class))).thenReturn(new RoleDTO());
        when(rolePermissionRepository.findByRoleId(1L))
                .thenReturn(List.of(rolePermission(1L, 10L)));
        when(permissionRepository.findAllById(List.of(10L)))
                .thenReturn(List.of(permission(10L, "读")));
        when(permissionMapper.toDTO(any(Permission.class))).thenReturn(new PermissionDTO());

        RoleDTO dto = service.getRoleByName("管理员");

        assertEquals(1, dto.getPermissions().size());
    }

    @Test
    @DisplayName("getRoleByName：名字不存在时抛 BusinessException(NOT_FOUND)")
    void getRoleByNameWhenMissingShouldThrowNotFound() {
        when(roleRepository.findByName("不存在")).thenReturn(Optional.empty());

        BusinessException ex =
                assertThrows(BusinessException.class, () -> service.getRoleByName("不存在"));

        assertEquals(ErrorCode.NOT_FOUND, ex.getErrorCode());
    }

    @Test
    @DisplayName("getRolePermissions：只透传权限映射，不查角色是否存在（现状行为，测试固定住）")
    void getRolePermissionsShouldMapPermissions() {
        when(rolePermissionRepository.findByRoleId(5L))
                .thenReturn(List.of(rolePermission(5L, 30L)));
        when(permissionRepository.findAllById(List.of(30L)))
                .thenReturn(List.of(permission(30L, "导出")));
        when(permissionMapper.toDTO(any(Permission.class))).thenReturn(new PermissionDTO());

        List<PermissionDTO> result = service.getRolePermissions(5L);

        assertEquals(1, result.size());
    }

    // ---------------------------------------------------------------- 创建

    @Test
    @DisplayName("createRole：同名已存在时抛 BusinessException(PARAM_ERROR) 且不写库")
    void createRoleWhenNameExistsShouldThrowParamError() {
        CreateRoleRequest request = new CreateRoleRequest();
        request.setName("管理员");
        request.setCreateBy(7L);
        when(roleRepository.findByName("管理员")).thenReturn(Optional.of(role(1L, "管理员")));

        BusinessException ex =
                assertThrows(BusinessException.class, () -> service.createRole(request));

        assertEquals(ErrorCode.PARAM_ERROR, ex.getErrorCode());
        verify(roleRepository, never()).save(any(Role.class));
    }

    @Test
    @DisplayName("createRole：成功时补齐审计字段（createBy/updateBy 同源）并保存")
    void createRoleShouldStampAuditFieldsAndSave() {
        CreateRoleRequest request = new CreateRoleRequest();
        request.setName("新角色");
        request.setDescription("新角色描述");
        request.setCreateBy(7L);
        when(roleRepository.findByName("新角色")).thenReturn(Optional.empty());
        when(roleRepository.save(any(Role.class))).thenAnswer(inv -> inv.getArgument(0));
        when(roleMapper.toDTO(any(Role.class)))
                .thenAnswer(
                        inv -> {
                            Role r = inv.getArgument(0);
                            RoleDTO dto = new RoleDTO();
                            dto.setName(r.getName());
                            dto.setCreateBy(r.getCreateBy());
                            dto.setUpdateBy(r.getUpdateBy());
                            return dto;
                        });

        RoleDTO dto = service.createRole(request);

        ArgumentCaptor<Role> captor = ArgumentCaptor.forClass(Role.class);
        verify(roleRepository).save(captor.capture());
        Role saved = captor.getValue();
        assertEquals("新角色", saved.getName());
        assertEquals("新角色描述", saved.getDescription());
        assertEquals(7L, saved.getCreateBy());
        assertEquals(7L, saved.getUpdateBy(), "创建时应把 createBy 同时写进 updateBy，避免审计字段空缺");
        assertNotNull(saved.getCreateTime());
        assertNotNull(saved.getUpdateTime());
        assertEquals("新角色", dto.getName());
    }

    // ---------------------------------------------------------------- 更新

    @Test
    @DisplayName("updateRole：ID 不存在时抛 NOT_FOUND 且不保存")
    void updateRoleWhenMissingShouldThrowNotFound() {
        UpdateRoleRequest request = new UpdateRoleRequest();
        request.setName("x");
        when(roleRepository.findById(9L)).thenReturn(Optional.empty());

        assertThrows(BusinessException.class, () -> service.updateRole(9L, request));

        verify(roleRepository, never()).save(any(Role.class));
    }

    @Test
    @DisplayName("updateRole：成功时覆盖名称/描述与 updateBy，并保留 createBy 不被改写")
    void updateRoleShouldOverwriteEditableFieldsOnly() {
        UpdateRoleRequest request = new UpdateRoleRequest();
        request.setName("改名后");
        request.setDescription("新描述");
        request.setUpdateBy(8L);
        Role existing = role(3L, "旧名");
        when(roleRepository.findById(3L)).thenReturn(Optional.of(existing));
        when(roleRepository.save(any(Role.class))).thenAnswer(inv -> inv.getArgument(0));
        when(roleMapper.toDTO(any(Role.class))).thenReturn(new RoleDTO());

        service.updateRole(3L, request);

        assertEquals("改名后", existing.getName());
        assertEquals("新描述", existing.getDescription());
        assertEquals(8L, existing.getUpdateBy());
        assertEquals(7L, existing.getCreateBy(), "更新不应改写创建人");
    }

    // ---------------------------------------------------------------- 删除

    @Test
    @DisplayName("deleteRole：ID 不存在时抛 NOT_FOUND，且不删任何关联")
    void deleteRoleWhenMissingShouldThrowNotFound() {
        when(roleRepository.findById(9L)).thenReturn(Optional.empty());

        assertThrows(BusinessException.class, () -> service.deleteRole(9L));

        verify(rolePermissionRepository, never()).deleteByRoleId(any());
        verify(roleRepository, never()).delete(any(Role.class));
    }

    @Test
    @DisplayName("deleteRole：必须先清 role_permission 再删 role（否则留下悬挂关联）")
    void deleteRoleShouldClearBindingsBeforeDelete() {
        Role existing = role(4L, "待删");
        when(roleRepository.findById(4L)).thenReturn(Optional.of(existing));

        service.deleteRole(4L);

        // 顺序断言：用 InOrder 固定"先清关联、后删主体"
        org.mockito.InOrder inOrder =
                org.mockito.Mockito.inOrder(rolePermissionRepository, roleRepository);
        inOrder.verify(rolePermissionRepository).deleteByRoleId(4L);
        inOrder.verify(roleRepository).delete(existing);
    }

    // ---------------------------------------------------------------- 分配权限

    @Test
    @DisplayName("assignPermissions：角色不存在时抛 NOT_FOUND，且不先清空既有绑定")
    void assignPermissionsWhenRoleMissingShouldThrowNotFound() {
        AssignPermissionsRequest request = new AssignPermissionsRequest();
        request.setPermissionIds(List.of(10L));
        when(roleRepository.findById(9L)).thenReturn(Optional.empty());

        assertThrows(BusinessException.class, () -> service.assignPermissions(9L, request));

        verify(rolePermissionRepository, never()).deleteByRoleId(any());
    }

    @Test
    @DisplayName("assignPermissions：任一权限不存在时抛 NOT_FOUND 且消息带上具体 ID")
    void assignPermissionsWhenPermissionMissingShouldThrowNotFoundWithId() {
        AssignPermissionsRequest request = new AssignPermissionsRequest();
        request.setPermissionIds(List.of(10L, 99L));
        request.setCreateBy(7L);
        when(roleRepository.findById(1L)).thenReturn(Optional.of(role(1L, "管理员")));
        when(permissionRepository.findById(10L)).thenReturn(Optional.of(permission(10L, "读")));
        when(permissionRepository.findById(99L)).thenReturn(Optional.empty());

        BusinessException ex =
                assertThrows(BusinessException.class, () -> service.assignPermissions(1L, request));

        assertEquals(ErrorCode.NOT_FOUND, ex.getErrorCode());
        assertTrue(
                String.valueOf(ex.getMessage()).contains("99"),
                "失败消息应指出是哪个权限 ID，便于排查；实际=" + ex.getMessage());
    }

    @Test
    @DisplayName("assignPermissions：成功时先清空再逐条写入，且每条都带上创建人")
    void assignPermissionsShouldReplaceExistingBindings() {
        AssignPermissionsRequest request = new AssignPermissionsRequest();
        request.setPermissionIds(List.of(10L, 20L));
        request.setCreateBy(7L);
        when(roleRepository.findById(1L)).thenReturn(Optional.of(role(1L, "管理员")));
        when(permissionRepository.findById(10L)).thenReturn(Optional.of(permission(10L, "读")));
        when(permissionRepository.findById(20L)).thenReturn(Optional.of(permission(20L, "写")));

        service.assignPermissions(1L, request);

        verify(rolePermissionRepository).deleteByRoleId(1L);
        ArgumentCaptor<RolePermission> captor = ArgumentCaptor.forClass(RolePermission.class);
        verify(rolePermissionRepository, times(2)).save(captor.capture());
        List<RolePermission> saved = captor.getAllValues();
        assertEquals(1L, saved.get(0).getRoleId());
        assertEquals(10L, saved.get(0).getPermissionId());
        assertEquals(7L, saved.get(0).getCreateBy());
        assertEquals(20L, saved.get(1).getPermissionId());
        assertNotNull(saved.get(0).getCreateTime());
        org.mockito.Mockito.inOrder(rolePermissionRepository)
                .verify(rolePermissionRepository)
                .deleteByRoleId(1L);
    }

    @Test
    @DisplayName("assignPermissions：空列表 = 只清空、不写入（用于撤销全部权限）")
    void assignPermissionsWithEmptyListShouldOnlyClear() {
        AssignPermissionsRequest request = new AssignPermissionsRequest();
        request.setPermissionIds(Collections.emptyList());
        request.setCreateBy(7L);
        when(roleRepository.findById(1L)).thenReturn(Optional.of(role(1L, "管理员")));

        service.assignPermissions(1L, request);

        verify(rolePermissionRepository).deleteByRoleId(1L);
        verify(rolePermissionRepository, never()).save(any(RolePermission.class));
        verify(permissionRepository, never()).findAllById(anyList());
    }
}
