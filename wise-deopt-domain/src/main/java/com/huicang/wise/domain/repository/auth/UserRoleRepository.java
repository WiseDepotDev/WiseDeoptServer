package com.huicang.wise.domain.repository.auth;

import com.huicang.wise.domain.auth.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 用户角色关联仓储接口
 *
 * @author WiseDepot
 * @version 0.0.22
 * @since 2026-02-27
 */
@Repository
public interface UserRoleRepository extends JpaRepository<UserRole, Long> {

    /**
     * 根据用户ID查询用户角色关联列表
     *
     * @param userId 用户ID
     * @return 用户角色关联列表
     */
    List<UserRole> findByUserId(Long userId);

    /**
     * 根据角色ID查询用户角色关联列表
     *
     * @param roleId 角色ID
     * @return 用户角色关联列表
     */
    List<UserRole> findByRoleId(Long roleId);

    /**
     * 根据用户ID删除用户角色关联
     *
     * @param userId 用户ID
     */
    void deleteByUserId(Long userId);
}