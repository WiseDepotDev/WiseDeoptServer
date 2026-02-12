package com.huicang.wise.infrastructure.repository.auth;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RoleRepository extends JpaRepository<RoleJpaEntity, Long> {
    Optional<RoleJpaEntity> findByRoleCode(String roleCode);
}
