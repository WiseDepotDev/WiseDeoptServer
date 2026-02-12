package com.huicang.wise.infrastructure.repository.auth;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserRoleRepository extends JpaRepository<UserRoleJpaEntity, Long> {
    List<UserRoleJpaEntity> findByUserId(Long userId);
}
