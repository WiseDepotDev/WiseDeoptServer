package com.huicang.wise.application.user;

import com.huicang.wise.application.role.RoleDTO;
import com.huicang.wise.application.role.RoleMapper;
import com.huicang.wise.common.api.ErrorCode;
import com.huicang.wise.common.exception.BusinessException;
import com.huicang.wise.domain.auth.Role;
import com.huicang.wise.domain.auth.UserRole;
import com.huicang.wise.infrastructure.persistence.repository.auth.RoleRepository;
import com.huicang.wise.infrastructure.persistence.repository.auth.UserRoleRepository;
import com.huicang.wise.infrastructure.redis.annotation.CacheEvict;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserRoleApplicationService {

    private final UserRoleRepository userRoleRepository;
    private final RoleRepository roleRepository;
    private final RoleMapper roleMapper;

    public UserRoleApplicationService(
            UserRoleRepository userRoleRepository,
            RoleRepository roleRepository,
            RoleMapper roleMapper) {
        this.userRoleRepository = userRoleRepository;
        this.roleRepository = roleRepository;
        this.roleMapper = roleMapper;
    }

    public List<RoleDTO> getUserRoles(Long userId) {
        List<UserRole> userRoles = userRoleRepository.findByUserId(userId);
        List<Long> roleIds =
                userRoles.stream().map(UserRole::getRoleId).collect(Collectors.toList());
        List<Role> roles = roleRepository.findAllById(roleIds);
        return roles.stream().map(roleMapper::toDTO).collect(Collectors.toList());
    }

    /**
     * 给用户重新分配角色。
     *
     * <p>清 `auth:permission` 的原因与 {@code RoleApplicationService.assignPermissions} 同： 权限检查的缓存键是
     * `用户名:权限码`，而这里换的是"这个人有哪些角色"。 精确清掉这一个用户的条目需要知道**他所有可能的权限码**（无法枚举）， 所以按前缀整类清 —— 这也是 `allEntries
     * = true` 唯一安全的用法（只清 `auth:permission:*`）。
     */
    @CacheEvict(prefix = "auth:permission", allEntries = true)
    @Transactional
    public void assignRoles(Long userId, AssignRolesRequest request) {
        userRoleRepository.deleteByUserId(userId);

        List<Long> distinctRoleIds =
                request.getRoleIds().stream().distinct().collect(Collectors.toList());

        for (Long roleId : distinctRoleIds) {
            Role role =
                    roleRepository
                            .findById(roleId)
                            .orElseThrow(
                                    () ->
                                            new BusinessException(
                                                    ErrorCode.NOT_FOUND, "角色不存在，ID: " + roleId));

            UserRole userRole = new UserRole();
            userRole.setUserId(userId);
            userRole.setRoleId(roleId);
            userRole.setCreateBy(userId);
            userRole.setCreateTime(LocalDateTime.now());
            userRoleRepository.save(userRole);
        }
    }

    @CacheEvict(prefix = "auth:permission", allEntries = true)
    @Transactional
    public void removeUserRoles(Long userId) {
        userRoleRepository.deleteByUserId(userId);
    }

    @CacheEvict(prefix = "auth:permission", allEntries = true)
    @Transactional
    public void removeUserRole(Long userId, Long roleId) {
        List<UserRole> userRoles = userRoleRepository.findByUserId(userId);
        for (UserRole userRole : userRoles) {
            if (userRole.getRoleId().equals(roleId)) {
                userRoleRepository.delete(userRole);
                break;
            }
        }
    }
}
