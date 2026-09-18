package com.huicang.wise.infrastructure.persistence.repository.user;

import com.huicang.wise.domain.user.UserSecurity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * 用户安全仓储接口
 *
 * @author WiseDepot
 * @version 0.0.24
 * @since 2026-03-03
 */
@Repository
public interface UserSecurityRepository extends JpaRepository<UserSecurity, Long> {

    /**
     * 根据用户ID查询用户安全信息
     *
     * @param userId 用户ID
     * @return 用户安全信息
     */
    Optional<UserSecurity> findByUserId(Long userId);

    /**
     * 检查用户ID是否存在
     *
     * @param userId 用户ID
     * @return 是否存在
     */
    boolean existsByUserId(Long userId);

    /**
     * 根据用户ID删除用户安全信息
     *
     * @param userId 用户ID
     */
    void deleteByUserId(Long userId);
}
