package com.huicang.wise.application.accesskey;

import com.huicang.wise.domain.auth.KeyAccessAuditLog;
import com.huicang.wise.domain.user.UserAccessKey;
import com.huicang.wise.application.accesskey.AccessKeyMapper;
import com.huicang.wise.application.accesskey.KeyAccessAuditLogMapper;
import com.huicang.wise.domain.repository.auth.KeyAccessAuditLogRepository;
import com.huicang.wise.domain.repository.user.UserAccessKeyRepository;
import com.huicang.wise.infrastructure.security.PasswordEncoder;
import com.huicang.wise.common.api.ErrorCode;
import com.huicang.wise.common.exception.BusinessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class AccessKeyApplicationService {

    private final UserAccessKeyRepository accessKeyRepository;
    private final KeyAccessAuditLogRepository auditLogRepository;
    private final AccessKeyMapper accessKeyMapper;
    private final KeyAccessAuditLogMapper auditLogMapper;
    private final PasswordEncoder passwordEncoder;

    public AccessKeyApplicationService(UserAccessKeyRepository accessKeyRepository,
                                     KeyAccessAuditLogRepository auditLogRepository,
                                     AccessKeyMapper accessKeyMapper,
                                     KeyAccessAuditLogMapper auditLogMapper,
                                     PasswordEncoder passwordEncoder) {
        this.accessKeyRepository = accessKeyRepository;
        this.auditLogRepository = auditLogRepository;
        this.accessKeyMapper = accessKeyMapper;
        this.auditLogMapper = auditLogMapper;
        this.passwordEncoder = passwordEncoder;
    }

    public List<AccessKeyDTO> getUserAccessKeys(Long userId) {
        List<UserAccessKey> accessKeys = accessKeyRepository.findByUserId(userId);
        return accessKeys.stream()
                .map(accessKeyMapper::toDTO)
                .collect(Collectors.toList());
    }

    public AccessKeyDTO getAccessKeyById(Long keyId) {
        UserAccessKey accessKey = accessKeyRepository.findById(keyId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "访问密钥不存在"));
        return accessKeyMapper.toDTO(accessKey);
    }

    /**
     * 按访问密钥（accessKey）反查记录。
     *
     * <p>供入口层（如审计拦截器）使用，避免入口层直接依赖仓储（STD-ARCH-02）。
     *
     * @param accessKey 访问密钥明文
     * @return 访问密钥 DTO；不存在时返回 null
     */
    public AccessKeyDTO findByAccessKey(String accessKey) {
        if (accessKey == null) {
            return null;
        }
        return accessKeyRepository.findByAccessKey(accessKey)
                .map(accessKeyMapper::toDTO)
                .orElse(null);
    }

    @Transactional
    public AccessKeyDTO createAccessKey(Long userId, CreateAccessKeyRequest request) {
        String accessKey = generateAccessKey();
        String secretKey = generateSecretKey();
        String secretKeyEnc = passwordEncoder.encode(secretKey);

        UserAccessKey userAccessKey = new UserAccessKey();
        userAccessKey.setUserId(userId);
        userAccessKey.setAccessKey(accessKey);
        userAccessKey.setSecretKeyEnc(secretKeyEnc);
        userAccessKey.setStatus(request.getStatus() != null ? request.getStatus() : (short) 1);
        userAccessKey.setDescription(request.getDescription());
        userAccessKey.setCreateBy(userId);
        userAccessKey.setCreateTime(LocalDateTime.now());
        userAccessKey.setUpdateBy(userId);
        userAccessKey.setUpdateTime(LocalDateTime.now());

        UserAccessKey saved = accessKeyRepository.save(userAccessKey);

        AccessKeyDTO dto = accessKeyMapper.toDTO(saved);
        dto.setSecretKeyEnc(secretKey);

        return dto;
    }

    @Transactional
    public void enableAccessKey(Long keyId) {
        UserAccessKey accessKey = accessKeyRepository.findById(keyId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "访问密钥不存在"));
        accessKey.setStatus((short) 1);
        accessKey.setUpdateBy(keyId);
        accessKey.setUpdateTime(LocalDateTime.now());
        accessKeyRepository.save(accessKey);
    }

    @Transactional
    public void disableAccessKey(Long keyId) {
        UserAccessKey accessKey = accessKeyRepository.findById(keyId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "访问密钥不存在"));
        accessKey.setStatus((short) 0);
        accessKey.setUpdateBy(keyId);
        accessKey.setUpdateTime(LocalDateTime.now());
        accessKeyRepository.save(accessKey);
    }

    @Transactional
    public void deleteAccessKey(Long keyId) {
        UserAccessKey accessKey = accessKeyRepository.findById(keyId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "访问密钥不存在"));
        accessKeyRepository.delete(accessKey);
    }

    public List<AccessKeyAuditLogDTO> getAccessKeyAuditLogs(Long keyId) {
        UserAccessKey accessKey = accessKeyRepository.findById(keyId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "访问密钥不存在"));
        List<KeyAccessAuditLog> logs = auditLogRepository.findByAccessKey(accessKey.getAccessKey());
        return logs.stream()
                .map(auditLogMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public void recordAccessLog(Long userId, String accessKey, String requestUri, String method,
                               String ipAddress, Short statusCode, String resultMessage, Integer durationMs) {
        KeyAccessAuditLog auditLog = new KeyAccessAuditLog();
        auditLog.setUserId(userId);
        auditLog.setAccessKey(accessKey);
        auditLog.setRequestUri(requestUri);
        auditLog.setMethod(method);
        auditLog.setIpAddress(ipAddress);
        auditLog.setStatusCode(statusCode);
        auditLog.setResultMessage(resultMessage);
        auditLog.setDurationMs(durationMs);
        auditLog.setRequestTime(LocalDateTime.now());
        auditLogRepository.save(auditLog);
    }

    @Transactional
    public void updateLastUsed(Long keyId) {
        UserAccessKey accessKey = accessKeyRepository.findById(keyId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "访问密钥不存在"));
        accessKey.setUpdateBy(keyId);
        accessKey.setUpdateTime(LocalDateTime.now());
        accessKeyRepository.save(accessKey);
    }

    private String generateAccessKey() {
        return "wdak_" + UUID.randomUUID().toString().replace("-", "").substring(0, 24);
    }

    private String generateSecretKey() {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
