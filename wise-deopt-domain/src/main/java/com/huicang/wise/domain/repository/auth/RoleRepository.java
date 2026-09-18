package com.huicang.wise.domain.repository.auth;

import com.huicang.wise.domain.auth.Role;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * 角色仓储接口
 *
 * @author WiseDepot
 * @version 0.0.22
 * @since 2026-02-27
 */
@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {

    /**
     * 根据角色名称查询角色
     *
     * @param name 角色名称
     * @return 角色信息
     */
    Optional<Role> findByName(String name);
}
