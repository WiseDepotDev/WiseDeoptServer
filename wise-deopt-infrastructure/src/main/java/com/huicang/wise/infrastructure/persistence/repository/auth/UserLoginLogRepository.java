package com.huicang.wise.infrastructure.persistence.repository.auth;

import com.huicang.wise.domain.auth.UserLoginLog;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * 用户登录日志仓储接口
 *
 * @author WiseDepot
 * @version 0.0.22
 * @since 2026-02-27
 */
@Repository
public interface UserLoginLogRepository extends JpaRepository<UserLoginLog, Long> {

    /**
     * 根据用户ID删除登录日志
     *
     * @param userId 用户ID
     */
    void deleteByUserId(Long userId);

    /**
     * 根据用户ID查询登录日志列表
     *
     * @param userId 用户ID
     * @return 登录日志列表
     */
    List<UserLoginLog> findByUserId(Long userId);

    /**
     * 根据用户ID分页查询登录日志
     *
     * @param userId 用户ID
     * @param pageable 分页参数
     * @return 登录日志分页结果
     */
    Page<UserLoginLog> findByUserId(Long userId, Pageable pageable);

    /**
     * 根据用户ID和登录结果查询登录日志列表
     *
     * @param userId 用户ID
     * @param success 登录结果
     * @return 登录日志列表
     */
    List<UserLoginLog> findByUserIdAndSuccess(Long userId, Short success);

    /**
     * 根据用户ID和登录类型查询登录日志列表
     *
     * @param userId 用户ID
     * @param loginType 登录类型
     * @return 登录日志列表
     */
    List<UserLoginLog> findByUserIdAndLoginType(Long userId, Short loginType);

    /**
     * 根据时间范围查询登录日志列表
     *
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 登录日志列表
     */
    List<UserLoginLog> findByLoginTimeBetween(LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 根据时间范围分页查询登录日志
     *
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @param pageable 分页参数
     * @return 登录日志分页结果
     */
    Page<UserLoginLog> findByLoginTimeBetween(
            LocalDateTime startTime, LocalDateTime endTime, Pageable pageable);

    /**
     * 根据用户ID和时间范围查询登录日志列表
     *
     * @param userId 用户ID
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 登录日志列表
     */
    @Query(
            "SELECT l FROM UserLoginLog l WHERE l.userId = :userId AND l.loginTime BETWEEN :startTime AND :endTime")
    List<UserLoginLog> findByUserIdAndLoginTimeBetween(
            @Param("userId") Long userId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);

    /**
     * 统计用户ID对应的登录日志数量
     *
     * @param userId 用户ID
     * @return 登录日志数量
     */
    long countByUserId(Long userId);

    /**
     * 统计用户ID和登录结果对应的登录日志数量
     *
     * @param userId 用户ID
     * @param success 登录结果
     * @return 登录日志数量
     */
    long countByUserIdAndSuccess(Long userId, Short success);

    /**
     * 查询最近N条登录日志
     *
     * @param userId 用户ID
     * @param limit 限制数量
     * @return 登录日志列表
     */
    @Query("SELECT l FROM UserLoginLog l WHERE l.userId = :userId ORDER BY l.loginTime DESC")
    List<UserLoginLog> findRecentByUserId(@Param("userId") Long userId, Pageable pageable);

    /**
     * 根据客户端IP地址查询登录日志列表
     *
     * @param ipAddress 客户端IP地址
     * @return 登录日志列表
     */
    List<UserLoginLog> findByIpAddress(String ipAddress);

    /**
     * 根据客户端IP地址分页查询登录日志
     *
     * @param ipAddress 客户端IP地址
     * @param pageable 分页参数
     * @return 登录日志分页结果
     */
    Page<UserLoginLog> findByIpAddress(String ipAddress, Pageable pageable);

    /**
     * 分页查询登录日志列表
     *
     * @param pageable 分页参数
     * @return 登录日志分页结果
     */
    Page<UserLoginLog> findAll(Pageable pageable);

    /**
     * 查询所有登录日志
     *
     * @return 登录日志列表
     */
    List<UserLoginLog> findAll();

    /**
     * 根据登录时间倒序查询登录日志列表
     *
     * @return 登录日志列表
     */
    @Query("SELECT l FROM UserLoginLog l ORDER BY l.loginTime DESC")
    List<UserLoginLog> findAllOrderByLoginTimeDesc();

    /**
     * 根据登录时间倒序分页查询登录日志
     *
     * @param pageable 分页参数
     * @return 登录日志分页结果
     */
    @Query("SELECT l FROM UserLoginLog l ORDER BY l.loginTime DESC")
    Page<UserLoginLog> findAllOrderByLoginTimeDesc(Pageable pageable);
}
