package com.huicang.wise.domain.repository.user;

import com.huicang.wise.domain.user.UserAccessKey;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * 用户访问密钥仓储接口
 *
 * @author WiseDepot
 * @version 0.0.22
 * @since 2026-02-27
 */
@Repository
public interface UserAccessKeyRepository extends JpaRepository<UserAccessKey, Long> {

    /**
     * 根据访问密钥查询密钥信息
     *
     * @param accessKey 访问密钥
     * @return 密钥信息
     */
    Optional<UserAccessKey> findByAccessKey(String accessKey);

    /**
     * 根据用户ID查询访问密钥列表
     *
     * @param userId 用户ID
     * @return 访问密钥列表
     */
    List<UserAccessKey> findByUserId(Long userId);

    /**
     * 根据用户ID删除访问密钥
     *
     * @param userId 用户ID
     */
    void deleteByUserId(Long userId);
}
