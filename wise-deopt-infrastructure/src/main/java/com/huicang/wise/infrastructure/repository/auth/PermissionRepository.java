package com.huicang.wise.infrastructure.repository.auth;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PermissionRepository extends JpaRepository<PermissionJpaEntity, Long> {
    Optional<PermissionJpaEntity> findByPermissionCode(String permissionCode);
}
