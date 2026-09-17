package com.huicang.wise.application.user;

import com.huicang.wise.application.role.RoleDTO;
import com.huicang.wise.application.role.RoleMapper;
import com.huicang.wise.domain.auth.Role;
import com.huicang.wise.domain.auth.UserRole;
import com.huicang.wise.domain.repository.auth.RoleRepository;
import com.huicang.wise.domain.repository.auth.UserRoleRepository;
import com.huicang.wise.common.api.ErrorCode;
import com.huicang.wise.common.exception.BusinessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserRoleApplicationService {

    private final UserRoleRepository userRoleRepository;
    private final RoleRepository roleRepository;
    private final RoleMapper roleMapper;

    public UserRoleApplicationService(UserRoleRepository userRoleRepository, RoleRepository roleRepository,
                                      RoleMapper roleMapper) {
        this.userRoleRepository = userRoleRepository;
        this.roleRepository = roleRepository;
        this.roleMapper = roleMapper;
    }

    public List<RoleDTO> getUserRoles(Long userId) {
        List<UserRole> userRoles = userRoleRepository.findByUserId(userId);
        List<Long> roleIds = userRoles.stream()
                .map(UserRole::getRoleId)
                .collect(Collectors.toList());
        List<Role> roles = roleRepository.findAllById(roleIds);
        return roles.stream()
                .map(roleMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public void assignRoles(Long userId, AssignRolesRequest request) {
        userRoleRepository.deleteByUserId(userId);

        List<Long> distinctRoleIds = request.getRoleIds().stream()
                .distinct()
                .collect(Collectors.toList());

        for (Long roleId : distinctRoleIds) {
            Role role = roleRepository.findById(roleId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "角色不存在，ID: " + roleId));

            UserRole userRole = new UserRole();
            userRole.setUserId(userId);
            userRole.setRoleId(roleId);
            userRole.setCreateBy(userId);
            userRole.setCreateTime(LocalDateTime.now());
            userRoleRepository.save(userRole);
        }
    }

    @Transactional
    public void removeUserRoles(Long userId) {
        userRoleRepository.deleteByUserId(userId);
    }

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
