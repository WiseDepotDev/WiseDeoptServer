package com.huicang.wise.infrastructure.persistence.repository.message;

import com.huicang.wise.domain.message.Message;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * 站内消息仓库。
 *
 * <p>它取代了 `MessageApplicationService` 里那个**进程内的 `ConcurrentHashMap`**：
 * 消息是业务事实（重启要还在、多实例要一致、要能查已读未读），所以 owner 必须是数据库。
 *
 * <p>为什么把"可选条件"写进一条 JPQL 而不是在 Java 里过滤：在 Java 里过滤要先把**全部**消息 查出来（`findAll()`），数据一多就是全表扫描 +
 * 全量传输。`(:x IS NULL OR …)` 这种写法让 过滤与分页都发生在数据库里。
 */
@Repository
public interface MessageRepository extends JpaRepository<Message, String> {

    /** 未读数（桥每 10s 轮询一次的那个数字）。 */
    long countByReceiverIdAndIsReadFalse(Long receiverId);

    /** 按条件分页查询，新的在前。 */
    @Query(
            "SELECT m FROM Message m WHERE "
                    + "(:receiverId IS NULL OR m.receiverId = :receiverId) AND "
                    + "(:type IS NULL OR m.type = :type) AND "
                    + "(:isRead IS NULL OR m.isRead = :isRead) "
                    + "ORDER BY m.createTime DESC")
    List<Message> findByConditions(
            @Param("receiverId") Long receiverId,
            @Param("type") String type,
            @Param("isRead") Boolean isRead,
            Pageable pageable);

    /**
     * 全部标记为已读。
     *
     * <p>用一条 UPDATE 而不是"查出来逐个改"：后者在消息多的账号上会把 N 条实体全部载入内存， 而且 N 次 UPDATE 让"标记全部已读"变成一个慢操作（用户点一下要等）。
     *
     * @return 受影响行数
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
            "UPDATE Message m SET m.isRead = true, m.readTime = :readTime "
                    + "WHERE m.receiverId = :receiverId AND m.isRead = false")
    int markAllAsRead(
            @Param("receiverId") Long receiverId,
            @Param("readTime") java.time.LocalDateTime readTime);

    /** 删除某人的全部消息。 */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    int deleteByReceiverId(Long receiverId);
}
