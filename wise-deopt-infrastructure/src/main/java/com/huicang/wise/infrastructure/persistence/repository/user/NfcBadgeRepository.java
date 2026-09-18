package com.huicang.wise.infrastructure.persistence.repository.user;

import com.huicang.wise.domain.user.NfcBadge;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * NFC工牌仓储接口
 *
 * @author WiseDepot
 * @version 0.0.24
 * @since 2026-03-03
 */
@Repository
public interface NfcBadgeRepository extends JpaRepository<NfcBadge, Long> {

    /**
     * 根据NFC UID查询工牌
     *
     * @param nfcUid NFC UID
     * @return 工牌信息
     */
    Optional<NfcBadge> findByNfcUid(String nfcUid);

    /**
     * 根据RFID查询工牌
     *
     * @param rfid RFID
     * @return 工牌信息
     */
    Optional<NfcBadge> findByRfid(String rfid);

    /**
     * 根据用户ID查询工牌
     *
     * @param userId 用户ID
     * @return 工牌信息
     */
    Optional<NfcBadge> findByUserId(Long userId);

    /**
     * 根据用户ID删除工牌
     *
     * @param userId 用户ID
     */
    void deleteByUserId(Long userId);

    /**
     * 根据NFC UID检查是否存在
     *
     * @param nfcUid NFC UID
     * @return 是否存在
     */
    boolean existsByNfcUid(String nfcUid);

    /**
     * 根据RFID检查是否存在
     *
     * @param rfid RFID
     * @return 是否存在
     */
    boolean existsByRfid(String rfid);
}
