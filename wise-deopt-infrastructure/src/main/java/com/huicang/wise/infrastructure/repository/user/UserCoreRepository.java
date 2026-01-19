package com.huicang.wise.infrastructure.repository.user;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * 类功能描述：用户核心信息JPA仓储
 *
 * @author xingchentye
 * @date 2026-01-19
 * @modified xingchentye 2026-01-19 提供基础查询方法
 */
public interface UserCoreRepository extends JpaRepository<UserCoreJpaEntity, Long> {

    /**
     * 方法功能描述：根据用户名查询用户
     *
     * @param username 登录名
     * @return 用户实体
     */
    Optional<UserCoreJpaEntity> findByUsername(String username);
}

