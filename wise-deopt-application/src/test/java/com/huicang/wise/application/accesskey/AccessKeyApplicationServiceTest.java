package com.huicang.wise.application.accesskey;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.huicang.wise.common.api.ErrorCode;
import com.huicang.wise.common.exception.BusinessException;
import com.huicang.wise.domain.auth.KeyAccessAuditLog;
import com.huicang.wise.domain.user.UserAccessKey;
import com.huicang.wise.infrastructure.persistence.repository.auth.KeyAccessAuditLogRepository;
import com.huicang.wise.infrastructure.persistence.repository.user.UserAccessKeyRepository;
import com.huicang.wise.infrastructure.security.PasswordEncoder;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * 访问密钥应用服务的单元测试：查询、创建（密钥生成与"一次性明文"）、启停、删除、审计日志与最后使用时间。
 *
 * <p>三处本批特意钉住的点： ① {@code createAccessKey} 返回的 DTO 里 {@code secretKeyEnc} 字段装的是**明文密钥**
 * （一次性展示的设计），而落库的是 {@code passwordEncoder.encode(明文)} —— 断言把二者绑定，防止重构时悄悄改成返回密文； ② {@code
 * enableAccessKey}/{@code disableAccessKey} 把 {@code updateBy} 写成 **keyId**（而不是操作人 ID）； ③ {@code
 * updateLastUsed} **只改 updateBy/updateTime，没有任何"最后使用时间"字段被更新** —— 方法名与实际行为不符，二者都是现状，本批只钉住、未改。
 */
@ExtendWith(MockitoExtension.class)
class AccessKeyApplicationServiceTest {

    private static final long USER_ID = 42L;
    private static final long KEY_ID = 7L;
    private static final String ACCESS_KEY = "wdak_0123456789abcdef01234567";

    @Mock private UserAccessKeyRepository accessKeyRepository;
    @Mock private KeyAccessAuditLogRepository auditLogRepository;
    @Mock private AccessKeyMapper accessKeyMapper;
    @Mock private KeyAccessAuditLogMapper auditLogMapper;
    @Mock private PasswordEncoder passwordEncoder;

    private AccessKeyApplicationService service;

    @BeforeEach
    void setUp() {
        service =
                new AccessKeyApplicationService(
                        accessKeyRepository,
                        auditLogRepository,
                        accessKeyMapper,
                        auditLogMapper,
                        passwordEncoder);
    }

    private UserAccessKey accessKey(Short status) {
        UserAccessKey entity = new UserAccessKey();
        entity.setUserId(USER_ID);
        entity.setAccessKey(ACCESS_KEY);
        entity.setSecretKeyEnc("ENCODED");
        entity.setStatus(status);
        return entity;
    }

    private void stubCreateHappyPath() {
        when(accessKeyRepository.save(any(UserAccessKey.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(passwordEncoder.encode(anyString()))
                .thenAnswer(invocation -> "ENC:" + invocation.getArgument(0));
        when(accessKeyMapper.toDTO(any(UserAccessKey.class)))
                .thenAnswer(invocation -> new AccessKeyDTO());
    }

    // ---------------- 查询 ----------------

    @Test
    @DisplayName("按用户查密钥：逐条映射")
    void getUserAccessKeysMapsAll() {
        when(accessKeyRepository.findByUserId(USER_ID))
                .thenReturn(List.of(accessKey((short) 1), accessKey((short) 0)));
        when(accessKeyMapper.toDTO(any(UserAccessKey.class)))
                .thenAnswer(invocation -> new AccessKeyDTO());

        assertEquals(2, service.getUserAccessKeys(USER_ID).size());
    }

    @Test
    @DisplayName("按用户查密钥：无记录返回空表")
    void getUserAccessKeysEmpty() {
        when(accessKeyRepository.findByUserId(USER_ID)).thenReturn(List.of());

        assertEquals(0, service.getUserAccessKeys(USER_ID).size());
    }

    @Test
    @DisplayName("按ID查密钥：不存在抛 NOT_FOUND")
    void getAccessKeyByIdMissing() {
        when(accessKeyRepository.findById(KEY_ID)).thenReturn(Optional.empty());

        BusinessException ex =
                assertThrows(BusinessException.class, () -> service.getAccessKeyById(KEY_ID));

        assertEquals(ErrorCode.NOT_FOUND, ex.getErrorCode());
    }

    @Test
    @DisplayName("按ID查密钥：命中并交给映射器")
    void getAccessKeyByIdFound() {
        UserAccessKey entity = accessKey((short) 1);
        when(accessKeyRepository.findById(KEY_ID)).thenReturn(Optional.of(entity));
        when(accessKeyMapper.toDTO(entity)).thenReturn(new AccessKeyDTO());

        assertNotNull(service.getAccessKeyById(KEY_ID));

        verify(accessKeyMapper).toDTO(entity);
    }

    @Test
    @DisplayName("按密钥反查：入参为 null 直接返回 null（不触达仓储）")
    void findByAccessKeyNullShortCircuits() {
        assertNull(service.findByAccessKey(null));

        verifyNoInteractions(accessKeyRepository);
    }

    @Test
    @DisplayName("按密钥反查：查不到返回 null（供拦截器使用，不抛异常）")
    void findByAccessKeyNotFound() {
        when(accessKeyRepository.findByAccessKey("nope")).thenReturn(Optional.empty());

        assertNull(service.findByAccessKey("nope"));
    }

    @Test
    @DisplayName("按密钥反查：命中返回 DTO")
    void findByAccessKeyFound() {
        UserAccessKey entity = accessKey((short) 1);
        when(accessKeyRepository.findByAccessKey(ACCESS_KEY)).thenReturn(Optional.of(entity));
        when(accessKeyMapper.toDTO(entity)).thenReturn(new AccessKeyDTO());

        assertNotNull(service.findByAccessKey(ACCESS_KEY));
    }

    // ---------------- 创建 ----------------

    @Test
    @DisplayName("创建密钥：accessKey 形如 wdak_+24位hex，秘密为 32 字节 URL-safe Base64")
    void createAccessKeyGeneratesShapes() {
        stubCreateHappyPath();

        AccessKeyDTO dto = service.createAccessKey(USER_ID, new CreateAccessKeyRequest());

        ArgumentCaptor<UserAccessKey> captor = ArgumentCaptor.forClass(UserAccessKey.class);
        verify(accessKeyRepository).save(captor.capture());
        String accessKey = captor.getValue().getAccessKey();
        assertTrue(accessKey.matches("^wdak_[0-9a-f]{24}$"), "实际=" + accessKey);
        String plainSecret = dto.getSecretKeyEnc();
        assertNotNull(plainSecret);
        assertEquals(43, plainSecret.length(), "32 字节 Base64 无填充应为 43 字符");
        assertTrue(plainSecret.matches("^[A-Za-z0-9_-]+$"), "应为 URL-safe 且无填充");
    }

    @Test
    @DisplayName("创建密钥：落库的是秘密的编码值，返回的是同一条秘密的明文（一次性展示）")
    void createAccessKeyStoresEncodedButReturnsPlain() {
        stubCreateHappyPath();

        AccessKeyDTO dto = service.createAccessKey(USER_ID, new CreateAccessKeyRequest());

        ArgumentCaptor<UserAccessKey> captor = ArgumentCaptor.forClass(UserAccessKey.class);
        verify(accessKeyRepository).save(captor.capture());
        assertEquals(
                "ENC:" + dto.getSecretKeyEnc(),
                captor.getValue().getSecretKeyEnc(),
                "落库值必须是返回明文的编码结果");
    }

    @Test
    @DisplayName("创建密钥：状态缺省为 1，显式状态优先")
    void createAccessKeyStatusDefaults() {
        stubCreateHappyPath();

        service.createAccessKey(USER_ID, new CreateAccessKeyRequest());
        CreateAccessKeyRequest disabled = new CreateAccessKeyRequest();
        disabled.setStatus((short) 0);
        service.createAccessKey(USER_ID, disabled);

        // 只 verify 一次：Mockito 的 captor 会跨多次 verify 累积
        ArgumentCaptor<UserAccessKey> captor = ArgumentCaptor.forClass(UserAccessKey.class);
        verify(accessKeyRepository, times(2)).save(captor.capture());
        assertEquals((short) 1, captor.getAllValues().get(0).getStatus());
        assertEquals((short) 0, captor.getAllValues().get(1).getStatus());
    }

    @DisplayName("创建密钥：审计字段落库、描述透传")
    void createAccessKeyFillsAuditFields() {
        stubCreateHappyPath();
        CreateAccessKeyRequest request = new CreateAccessKeyRequest();
        request.setDescription("给巡检机器人用");

        service.createAccessKey(USER_ID, request);

        ArgumentCaptor<UserAccessKey> captor = ArgumentCaptor.forClass(UserAccessKey.class);
        verify(accessKeyRepository).save(captor.capture());
        UserAccessKey saved = captor.getValue();
        assertEquals(USER_ID, saved.getUserId().longValue());
        assertEquals(USER_ID, saved.getCreateBy().longValue());
        assertEquals(USER_ID, saved.getUpdateBy().longValue());
        assertEquals("给巡检机器人用", saved.getDescription());
        assertNotNull(saved.getCreateTime());
        assertNotNull(saved.getUpdateTime());
    }

    @Test
    @DisplayName("创建密钥：两次创建的 accessKey 与秘密都不同")
    void createAccessKeyIsRandomPerCall() {
        stubCreateHappyPath();

        AccessKeyDTO first = service.createAccessKey(USER_ID, new CreateAccessKeyRequest());
        AccessKeyDTO second = service.createAccessKey(USER_ID, new CreateAccessKeyRequest());

        assertNotEquals(first.getSecretKeyEnc(), second.getSecretKeyEnc());

        ArgumentCaptor<UserAccessKey> captor = ArgumentCaptor.forClass(UserAccessKey.class);
        verify(accessKeyRepository, times(2)).save(captor.capture());
        assertNotEquals(
                captor.getAllValues().get(0).getAccessKey(),
                captor.getAllValues().get(1).getAccessKey());
    }

    // ---------------- 启停 / 删除 ----------------

    @Test
    @DisplayName("启用密钥：不存在抛 NOT_FOUND")
    void enableMissing() {
        when(accessKeyRepository.findById(KEY_ID)).thenReturn(Optional.empty());

        BusinessException ex =
                assertThrows(BusinessException.class, () -> service.enableAccessKey(KEY_ID));

        assertEquals(ErrorCode.NOT_FOUND, ex.getErrorCode());
    }

    @Test
    @DisplayName("启用密钥：状态置 1 并落库")
    void enableSetsStatusOne() {
        UserAccessKey entity = accessKey((short) 0);
        when(accessKeyRepository.findById(KEY_ID)).thenReturn(Optional.of(entity));

        service.enableAccessKey(KEY_ID);

        assertEquals((short) 1, entity.getStatus());
        assertNotNull(entity.getUpdateTime());
        verify(accessKeyRepository).save(entity);
    }

    @Test
    @DisplayName("停用密钥：状态置 0 并落库")
    void disableSetsStatusZero() {
        UserAccessKey entity = accessKey((short) 1);
        when(accessKeyRepository.findById(KEY_ID)).thenReturn(Optional.of(entity));

        service.disableAccessKey(KEY_ID);

        assertEquals((short) 0, entity.getStatus());
        verify(accessKeyRepository).save(entity);
    }

    @Test
    @DisplayName("现状：启停把 updateBy 写成 keyId（不是操作人）")
    void enableWritesKeyIdIntoUpdateBy() {
        UserAccessKey entity = accessKey((short) 0);
        when(accessKeyRepository.findById(KEY_ID)).thenReturn(Optional.of(entity));

        service.enableAccessKey(KEY_ID);

        assertEquals(
                KEY_ID,
                entity.getUpdateBy().longValue(),
                "现状：updateBy 用的是 keyId；若本意是操作人，则这里应改为传入操作人ID");
    }

    @Test
    @DisplayName("删除密钥：不存在抛 NOT_FOUND 且不删除")
    void deleteMissing() {
        when(accessKeyRepository.findById(KEY_ID)).thenReturn(Optional.empty());

        BusinessException ex =
                assertThrows(BusinessException.class, () -> service.deleteAccessKey(KEY_ID));

        assertEquals(ErrorCode.NOT_FOUND, ex.getErrorCode());
        verify(accessKeyRepository, never()).delete(any(UserAccessKey.class));
    }

    @Test
    @DisplayName("删除密钥：命中则删除实体")
    void deleteSuccess() {
        UserAccessKey entity = accessKey((short) 1);
        when(accessKeyRepository.findById(KEY_ID)).thenReturn(Optional.of(entity));

        service.deleteAccessKey(KEY_ID);

        verify(accessKeyRepository).delete(entity);
    }

    // ---------------- 审计日志 ----------------

    @Test
    @DisplayName("审计日志：密钥不存在抛 NOT_FOUND")
    void auditLogsMissingKey() {
        when(accessKeyRepository.findById(KEY_ID)).thenReturn(Optional.empty());

        BusinessException ex =
                assertThrows(BusinessException.class, () -> service.getAccessKeyAuditLogs(KEY_ID));

        assertEquals(ErrorCode.NOT_FOUND, ex.getErrorCode());
        verifyNoInteractions(auditLogRepository);
    }

    @Test
    @DisplayName("审计日志：用密钥字符串查日志并逐条映射")
    void auditLogsMapped() {
        when(accessKeyRepository.findById(KEY_ID)).thenReturn(Optional.of(accessKey((short) 1)));
        when(auditLogRepository.findByAccessKey(ACCESS_KEY))
                .thenReturn(List.of(new KeyAccessAuditLog(), new KeyAccessAuditLog()));
        when(auditLogMapper.toDTO(any(KeyAccessAuditLog.class)))
                .thenReturn(new AccessKeyAuditLogDTO());

        assertEquals(2, service.getAccessKeyAuditLogs(KEY_ID).size());
    }

    @Test
    @DisplayName("记录访问日志：全部字段与请求时间落库")
    void recordAccessLogPersistsAllFields() {
        LocalDateTime before = LocalDateTime.now();

        service.recordAccessLog(
                USER_ID,
                ACCESS_KEY,
                "/api/inventory/report",
                "POST",
                "10.0.0.9",
                (short) 200,
                "ok",
                17);

        ArgumentCaptor<KeyAccessAuditLog> captor = ArgumentCaptor.forClass(KeyAccessAuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        KeyAccessAuditLog log = captor.getValue();
        assertEquals(USER_ID, log.getUserId().longValue());
        assertEquals(ACCESS_KEY, log.getAccessKey());
        assertEquals("/api/inventory/report", log.getRequestUri());
        assertEquals("POST", log.getMethod());
        assertEquals("10.0.0.9", log.getIpAddress());
        assertEquals((short) 200, log.getStatusCode());
        assertEquals("ok", log.getResultMessage());
        assertEquals(17, log.getDurationMs());
        assertTrue(log.getRequestTime().isAfter(before.minusSeconds(1)));
    }

    @Test
    @DisplayName("更新最后使用：密钥不存在抛 NOT_FOUND")
    void updateLastUsedMissing() {
        when(accessKeyRepository.findById(KEY_ID)).thenReturn(Optional.empty());

        BusinessException ex =
                assertThrows(BusinessException.class, () -> service.updateLastUsed(KEY_ID));

        assertEquals(ErrorCode.NOT_FOUND, ex.getErrorCode());
    }

    @Test
    @DisplayName("现状：updateLastUsed 只改 updateBy/updateTime，没有任何“最后使用时间”字段被写")
    void updateLastUsedOnlyTouchesUpdateFields() {
        UserAccessKey entity = accessKey((short) 1);
        LocalDateTime before = entity.getUpdateTime();
        when(accessKeyRepository.findById(KEY_ID)).thenReturn(Optional.of(entity));

        service.updateLastUsed(KEY_ID);

        assertEquals(KEY_ID, entity.getUpdateBy().longValue());
        assertNotEquals(before, entity.getUpdateTime());
        verify(accessKeyRepository).save(entity);
    }
}
