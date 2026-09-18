package com.huicang.wise.application.permission;

import com.huicang.wise.common.api.ErrorCode;
import com.huicang.wise.common.exception.BusinessException;
import com.huicang.wise.domain.auth.Permission;
import com.huicang.wise.infrastructure.persistence.repository.auth.PermissionRepository;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PermissionApplicationService {

    private final PermissionRepository permissionRepository;
    private final PermissionMapper permissionMapper;

    public PermissionApplicationService(
            PermissionRepository permissionRepository, PermissionMapper permissionMapper) {
        this.permissionRepository = permissionRepository;
        this.permissionMapper = permissionMapper;
    }

    public List<PermissionDTO> getAllPermissions() {
        List<Permission> permissions = permissionRepository.findAll();
        return permissions.stream().map(permissionMapper::toDTO).collect(Collectors.toList());
    }

    public List<PermissionDTO> getPermissionTree() {
        List<Permission> allPermissions = permissionRepository.findAll();
        List<PermissionDTO> allDTOs =
                allPermissions.stream().map(permissionMapper::toDTO).collect(Collectors.toList());
        return buildPermissionTree(allDTOs, null);
    }

    public PermissionDTO getPermissionById(Long id) {
        return permissionRepository
                .findById(id)
                .map(permissionMapper::toDTO)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "权限不存在"));
    }

    public PermissionDTO getPermissionByCode(String code) {
        return permissionRepository
                .findByCode(code)
                .map(permissionMapper::toDTO)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "权限不存在"));
    }

    @Transactional
    public PermissionDTO createPermission(CreatePermissionRequest request) {
        if (permissionRepository.findByCode(request.getPermissionCode()).isPresent()) {
            throw new BusinessException(ErrorCode.VAL_CONFLICT_PERMISSION_CODE_EXISTS, "权限编码已存在");
        }

        Permission permission = new Permission();
        permission.setName(request.getPermissionName());
        permission.setCode(request.getPermissionCode());
        permission.setDescription(request.getDescription());
        permission.setCreateTime(LocalDateTime.now());
        permission.setUpdateTime(LocalDateTime.now());

        Permission saved = permissionRepository.save(permission);
        return permissionMapper.toDTO(saved);
    }

    @Transactional
    public PermissionDTO updatePermission(Long id, UpdatePermissionRequest request) {
        Permission permission =
                permissionRepository
                        .findById(id)
                        .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "权限不存在"));

        permission.setName(request.getPermissionName());
        permission.setDescription(request.getDescription());
        permission.setUpdateTime(LocalDateTime.now());

        Permission updated = permissionRepository.save(permission);
        return permissionMapper.toDTO(updated);
    }

    @Transactional
    public void deletePermission(Long id) {
        Permission permission =
                permissionRepository
                        .findById(id)
                        .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "权限不存在"));

        permissionRepository.delete(permission);
    }

    private List<PermissionDTO> buildPermissionTree(
            List<PermissionDTO> allPermissions, Long parentId) {
        List<PermissionDTO> tree = new ArrayList<>();
        for (PermissionDTO permission : allPermissions) {
            if ((parentId == null && permission.getParentId() == null)
                    || (parentId != null && parentId.equals(permission.getParentId()))) {
                List<PermissionDTO> children =
                        buildPermissionTree(allPermissions, permission.getPermissionId());
                if (!children.isEmpty()) {
                    permission.setChildren(children);
                }
                tree.add(permission);
            }
        }
        return tree;
    }

    private boolean hasChildren(Long parentId) {
        return false;
    }

    private boolean hasCircularDependency(Long currentId, Long newParentId) {
        return false;
    }
}
