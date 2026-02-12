package com.huicang.wise.infrastructure.repository.auth;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RolePermissionRepository extends JpaRepository<RolePermissionJpaEntity, Long> {
    List<RolePermissionJpaEntity> findByRoleId(Long roleId);
}
