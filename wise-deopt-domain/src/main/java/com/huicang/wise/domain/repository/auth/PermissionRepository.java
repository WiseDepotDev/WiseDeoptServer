package com.huicang.wise.domain.repository.auth;

import com.huicang.wise.domain.auth.Permission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 权限仓储接口
 *
 * @author WiseDepot
 * @version 0.0.26
 * @since 2026-02-27
 */
@Repository
public interface PermissionRepository extends JpaRepository<Permission, Long> {

    /**
     * 根据权限ID查询权限
     *
     * @param permissionId 权限ID
     * @return 权限
     */
    Optional<Permission> findByPermissionId(Long permissionId);

    /**
     * 根据权限编码查询权限
     *
     * @param code 权限编码
     * @return 权限
     */
    Optional<Permission> findByCode(String code);

    /**
     * 根据权限名称查询权限
     *
     * @param name 权限名称
     * @return 权限
     */
    Optional<Permission> findByName(String name);

    /**
     * 根据权限ID删除权限
     *
     * @param permissionId 权限ID
     */
    void deleteByPermissionId(Long permissionId);
}