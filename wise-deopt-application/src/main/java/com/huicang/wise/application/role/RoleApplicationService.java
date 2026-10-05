package com.huicang.wise.application.role;

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
import com.huicang.wise.infrastructure.redis.annotation.CacheEvict;
import com.huicang.wise.infrastructure.redis.annotation.Cacheable;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RoleApplicationService {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final RolePermissionRepository rolePermissionRepository;
    private final RoleMapper roleMapper;
    private final PermissionMapper permissionMapper;

    public RoleApplicationService(
            RoleRepository roleRepository,
            PermissionRepository permissionRepository,
            RolePermissionRepository rolePermissionRepository,
            RoleMapper roleMapper,
            PermissionMapper permissionMapper) {
        this.roleRepository = roleRepository;
        this.permissionRepository = permissionRepository;
        this.rolePermissionRepository = rolePermissionRepository;
        this.roleMapper = roleMapper;
        this.permissionMapper = permissionMapper;
    }

    public List<RoleDTO> getAllRoles() {
        List<Role> roles = roleRepository.findAll();
        return roles.stream().map(roleMapper::toDTO).collect(Collectors.toList());
    }

    @Cacheable(prefix = "role", key = "#id", timeout = 3600)
    public RoleDTO getRoleById(Long id) {
        Role role =
                roleRepository
                        .findById(id)
                        .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "角色不存在"));
        RoleDTO dto = roleMapper.toDTO(role);
        List<Permission> permissions = getPermissionsByRoleId(id);
        dto.setPermissions(
                permissions.stream().map(permissionMapper::toDTO).collect(Collectors.toList()));
        return dto;
    }

    @Cacheable(prefix = "role:name", key = "#name", timeout = 3600)
    public RoleDTO getRoleByName(String name) {
        Role role =
                roleRepository
                        .findByName(name)
                        .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "角色不存在"));
        RoleDTO dto = roleMapper.toDTO(role);
        List<Permission> permissions = getPermissionsByRoleId(role.getRoleId());
        dto.setPermissions(
                permissions.stream().map(permissionMapper::toDTO).collect(Collectors.toList()));
        return dto;
    }

    @Transactional
    public RoleDTO createRole(CreateRoleRequest request) {
        if (roleRepository.findByName(request.getName()).isPresent()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "角色名称已存在");
        }

        Role role = new Role();
        role.setName(request.getName());
        role.setDescription(request.getDescription());
        role.setCreateTime(LocalDateTime.now());
        role.setCreateBy(request.getCreateBy());
        role.setUpdateTime(LocalDateTime.now());
        role.setUpdateBy(request.getCreateBy());

        Role saved = roleRepository.save(role);
        return roleMapper.toDTO(saved);
    }

    /**
     * 改角色本身（名字/描述）。
     *
     * <p>为什么也要清 `auth:permission`：管理员判定是按**角色名**做的（`AuthApplicationService`
     * 里认"超级管理员/管理员/ADMIN"），改名字等于改权限结论 —— 缓存里那个"通过"必须作废。
     */
    @CacheEvict(prefix = "auth:permission", allEntries = true)
    @Transactional
    public RoleDTO updateRole(Long id, UpdateRoleRequest request) {
        Role role =
                roleRepository
                        .findById(id)
                        .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "角色不存在"));

        role.setName(request.getName());
        role.setDescription(request.getDescription());
        role.setUpdateTime(LocalDateTime.now());
        role.setUpdateBy(request.getUpdateBy());

        Role updated = roleRepository.save(role);
        return roleMapper.toDTO(updated);
    }

    // 删角色同时影响两类缓存：这个角色自己的条目、以及所有"靠它拿权限"的人。
    // 两个 `@CacheEvict` 都是**声明式**的（注解可重复）—— 不在这里手写 RedisCacheUtils 调用：
    // 那种静态服务定位器会让单元测试在没容器时直接 NPE（`RoleApplicationServiceTest` 就是纯单测）。
    @CacheEvict(prefix = "auth:permission", allEntries = true)
    @CacheEvict(prefix = "role", key = "#id", allEntries = false)
    @Transactional
    public void deleteRole(Long id) {
        Role role =
                roleRepository
                        .findById(id)
                        .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "角色不存在"));

        rolePermissionRepository.deleteByRoleId(id);
        roleRepository.delete(role);
    }

    /**
     * 给角色重新分配权限 —— **权限缓存必须整体作废**。
     *
     * <p>缓存键是 `用户名:权限码`（按用户维度），而这里变的是"某个角色有哪些权限"： 一个角色可能挂在很多用户身上，所以只能整类清（`allEntries = true`，按前缀
     * `auth:permission:*` 清， 不会碰到登录锁定/限流那些**不属于缓存**的状态 —— 那一点由 `RedisCacheAspect` 保证）。
     *
     * <p>为什么必须清：不清的话"刚被收回权限的人"最多还能用 15 分钟（缓存 TTL）。 权限收紧要立即生效，这是安全语义，不是性能取舍。
     */
    @CacheEvict(prefix = "auth:permission", allEntries = true)
    @Transactional
    public void assignPermissions(Long roleId, AssignPermissionsRequest request) {
        Role role =
                roleRepository
                        .findById(roleId)
                        .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "角色不存在"));

        rolePermissionRepository.deleteByRoleId(roleId);

        for (Long permissionId : request.getPermissionIds()) {
            Permission permission =
                    permissionRepository
                            .findById(permissionId)
                            .orElseThrow(
                                    () ->
                                            new BusinessException(
                                                    ErrorCode.NOT_FOUND,
                                                    "权限不存在，ID: " + permissionId));

            RolePermission rolePermission = new RolePermission();
            rolePermission.setRoleId(roleId);
            rolePermission.setPermissionId(permissionId);
            rolePermission.setCreateTime(LocalDateTime.now());
            rolePermission.setCreateBy(request.getCreateBy());
            rolePermissionRepository.save(rolePermission);
        }
    }

    public List<PermissionDTO> getRolePermissions(Long roleId) {
        List<Permission> permissions = getPermissionsByRoleId(roleId);
        return permissions.stream().map(permissionMapper::toDTO).collect(Collectors.toList());
    }

    private List<Permission> getPermissionsByRoleId(Long roleId) {
        List<RolePermission> rolePermissions = rolePermissionRepository.findByRoleId(roleId);
        List<Long> permissionIds =
                rolePermissions.stream()
                        .map(RolePermission::getPermissionId)
                        .collect(Collectors.toList());
        return permissionRepository.findAllById(permissionIds);
    }
}
