package com.huicang.wise.application.permission;

import com.huicang.wise.domain.auth.Permission;
import com.huicang.wise.domain.auth.Role;
import com.huicang.wise.domain.auth.RolePermission;
import com.huicang.wise.domain.auth.UserRole;
import com.huicang.wise.domain.repository.auth.PermissionRepository;
import com.huicang.wise.domain.repository.auth.RolePermissionRepository;
import com.huicang.wise.domain.repository.auth.RoleRepository;
import com.huicang.wise.domain.repository.auth.UserRoleRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class PermissionService {

    private final UserRoleRepository userRoleRepository;
    private final RolePermissionRepository rolePermissionRepository;
    private final PermissionRepository permissionRepository;
    private final RoleRepository roleRepository;

    public PermissionService(UserRoleRepository userRoleRepository,
                              RolePermissionRepository rolePermissionRepository,
                              PermissionRepository permissionRepository,
                              RoleRepository roleRepository) {
        this.userRoleRepository = userRoleRepository;
        this.rolePermissionRepository = rolePermissionRepository;
        this.permissionRepository = permissionRepository;
        this.roleRepository = roleRepository;
    }

    public boolean hasPermission(Long userId, String permissionCode) {
        // 1. 检查是否为超级管理员
        if (isSuperAdmin(userId)) {
            return true;
        }
        
        // 2. 检查具体权限
        Set<String> userPermissions = getUserPermissions(userId);
        return userPermissions.contains(permissionCode);
    }

    private boolean isSuperAdmin(Long userId) {
        List<UserRole> userRoles = userRoleRepository.findByUserId(userId);
        for (UserRole userRole : userRoles) {
            Role role = roleRepository.findById(userRole.getRoleId()).orElse(null);
            if (role != null && ("超级管理员".equals(role.getName()) || "管理员".equals(role.getName()) || "ADMIN".equalsIgnoreCase(role.getName()))) {
                return true;
            }
        }
        return false;
    }

    public Set<String> getUserPermissions(Long userId) {
        Set<String> permissions = new HashSet<>();

        List<UserRole> userRoles = userRoleRepository.findByUserId(userId);
        for (UserRole userRole : userRoles) {
            List<RolePermission> rolePermissions = rolePermissionRepository.findByRoleId(userRole.getRoleId());
            for (RolePermission rolePermission : rolePermissions) {
                Permission permission = permissionRepository.findById(rolePermission.getPermissionId()).orElse(null);
                if (permission != null) {
                    permissions.add(permission.getCode());
                }
            }
        }

        return permissions;
    }

    public List<Permission> getAllPermissions() {
        return permissionRepository.findAll();
    }

    public List<Permission> getPermissionsByUserId(Long userId) {
        Set<String> permissionCodes = getUserPermissions(userId);
        List<Permission> allPermissions = permissionRepository.findAll();
        List<Permission> userPermissions = new ArrayList<>();
        for (Permission permission : allPermissions) {
            if (permissionCodes.contains(permission.getCode())) {
                userPermissions.add(permission);
            }
        }
        return userPermissions;
    }

    public List<Role> getRolesByUserId(Long userId) {
        List<UserRole> userRoles = userRoleRepository.findByUserId(userId);
        List<Long> roleIds = new ArrayList<>();
        for (UserRole userRole : userRoles) {
            roleIds.add(userRole.getRoleId());
        }
        return roleRepository.findAllById(roleIds);
    }
}
