package com.huicang.wise.domain.repository.auth;

import com.huicang.wise.domain.auth.RolePermission;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * 角色权限关联仓储接口
 *
 * @author WiseDepot
 * @version 0.0.22
 * @since 2026-02-27
 */
@Repository
public interface RolePermissionRepository extends JpaRepository<RolePermission, Long> {

    /**
     * 根据角色ID查询角色权限关联列表
     *
     * @param roleId 角色ID
     * @return 角色权限关联列表
     */
    List<RolePermission> findByRoleId(Long roleId);

    /**
     * 根据角色ID列表查询角色权限关联列表
     *
     * @param roleIds 角色ID列表
     * @return 角色权限关联列表
     */
    List<RolePermission> findByRoleIdIn(List<Long> roleIds);

    /**
     * 根据权限ID查询角色权限关联列表
     *
     * @param permissionId 权限ID
     * @return 角色权限关联列表
     */
    List<RolePermission> findByPermissionId(Long permissionId);

    /**
     * 根据角色ID删除角色权限关联
     *
     * @param roleId 角色ID
     */
    void deleteByRoleId(Long roleId);
}
