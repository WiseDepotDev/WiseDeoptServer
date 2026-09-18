package com.huicang.wise.domain.repository.user;

import com.huicang.wise.domain.user.UserCore;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * 用户核心仓储接口
 *
 * @author WiseDepot
 * @version 0.0.22
 * @since 2026-02-27
 */
@Repository
public interface UserCoreRepository extends JpaRepository<UserCore, Long> {

    /**
     * 根据用户名查询用户
     *
     * @param username 用户名
     * @return 用户信息
     */
    Optional<UserCore> findByUsername(String username);

    /**
     * 根据状态查询用户列表
     *
     * @param status 状态
     * @return 用户列表
     */
    List<UserCore> findByStatus(Integer status);

    /**
     * 根据状态分页查询用户
     *
     * @param status 状态
     * @param pageable 分页参数
     * @return 用户分页结果
     */
    Page<UserCore> findByStatus(Integer status, Pageable pageable);

    /**
     * 统计状态对应的用户数量
     *
     * @param status 状态
     * @return 用户数量
     */
    long countByStatus(Integer status);

    /**
     * 检查用户名是否存在
     *
     * @param username 用户名
     * @return 是否存在
     */
    boolean existsByUsername(String username);

    /**
     * 根据用户ID列表查询用户列表
     *
     * @param userIds 用户ID列表
     * @return 用户列表
     */
    List<UserCore> findByUserIdIn(List<Long> userIds);

    /**
     * 分页查询用户列表
     *
     * @param pageable 分页参数
     * @return 用户分页结果
     */
    Page<UserCore> findAll(Pageable pageable);
}
