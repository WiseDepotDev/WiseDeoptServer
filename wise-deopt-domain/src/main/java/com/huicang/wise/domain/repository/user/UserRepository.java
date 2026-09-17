package com.huicang.wise.domain.repository.user;

import com.huicang.wise.domain.user.UserCore;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 用户核心仓储接口
 *
 * @author WiseDepot
 * @version 0.0.26
 * @since 2026-02-27
 */
@Repository
public interface UserRepository {

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
     * @param status 用户状态
     * @return 用户列表
     */
    List<UserCore> findByStatus(Integer status);

    /**
     * 分页查询用户列表
     *
     * @param pageable 分页参数
     * @return 用户分页结果
     */
    Page<UserCore> findAll(Pageable pageable);

    /**
     * 根据用户名模糊查询用户
     *
     * @param username 用户名（支持模糊匹配）
     * @param pageable 分页参数
     * @return 用户分页结果
     */
    Page<UserCore> findByUsernameContaining(@Param("username") String username, Pageable pageable);

    /**
     * 检查用户名是否存在
     *
     * @param username 用户名
     * @return 是否存在
     */
    boolean existsByUsername(String username);

    /**
     * 根据状态统计用户数量
     *
     * @param status 用户状态
     * @return 用户数量
     */
    long countByStatus(Integer status);

    /**
     * 保存用户
     *
     * @param entity 用户实体
     * @return 保存后的用户实体
     */
    <S extends UserCore> S save(S entity);

    /**
     * 根据ID查询用户
     *
     * @param id 用户ID
     * @return 用户信息
     */
    Optional<UserCore> findById(Long id);

    /**
     * 检查ID是否存在
     *
     * @param id 用户ID
     * @return 是否存在
     */
    boolean existsById(Long id);

    /**
     * 根据ID删除用户
     *
     * @param id 用户ID
     */
    void deleteById(Long id);

    /**
     * 删除用户
     *
     * @param entity 用户实体
     */
    void delete(UserCore entity);

    /**
     * 删除所有用户
     */
    void deleteAll();

    /**
     * 查询所有用户
     *
     * @return 用户列表
     */
    List<UserCore> findAll();

    /**
     * 根据ID列表查询用户
     *
     * @param ids 用户ID列表
     * @return 用户列表
     */
    List<UserCore> findAllById(Iterable<Long> ids);

    /**
     * 统计用户数量
     *
     * @return 用户数量
     */
    long count();
}
