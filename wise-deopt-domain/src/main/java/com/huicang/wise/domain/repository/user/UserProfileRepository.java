package com.huicang.wise.domain.repository.user;

import com.huicang.wise.domain.user.UserProfile;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * 用户档案仓储接口
 *
 * @author WiseDepot
 * @version 0.0.22
 * @since 2026-02-27
 */
@Repository
public interface UserProfileRepository extends JpaRepository<UserProfile, Long> {

    /**
     * 根据用户ID查询用户档案
     *
     * @param userId 用户ID
     * @return 用户档案
     */
    Optional<UserProfile> findByUserId(Long userId);

    /**
     * 根据邮箱查询用户档案
     *
     * @param email 邮箱
     * @return 用户档案
     */
    Optional<UserProfile> findByEmail(String email);

    /**
     * 检查邮箱是否存在
     *
     * @param email 邮箱
     * @return 是否存在
     */
    boolean existsByEmail(String email);

    /**
     * 根据用户ID删除用户档案
     *
     * @param userId 用户ID
     */
    void deleteByUserId(Long userId);
}
