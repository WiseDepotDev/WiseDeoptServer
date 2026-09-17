package com.huicang.wise.infrastructure.security;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import com.huicang.wise.common.exception.BusinessException;
import com.huicang.wise.common.api.ErrorCode;

@Component
@Slf4j
public class DataEncryptionService {

    @Value("${encryption.key:wise-depot-encryption-key-256-bit-secure-key-for-data-encryption}")
    private String encryptionKey;

    private static DataEncryptionService instance;

    private static final String AES_GCM_ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_TAG_LENGTH = 128;
    private static final int GCM_IV_LENGTH = 12;
    private static final int KEY_LENGTH = 256;

    @PostConstruct
    public void init() {
        instance = this;
    }

    public static DataEncryptionService getInstance() {
        return instance;
    }

    private SecretKey getSecretKey() {
        byte[] keyBytes = encryptionKey.getBytes(StandardCharsets.UTF_8);
        byte[] key = new byte[32];
        System.arraycopy(keyBytes, 0, key, 0, Math.min(keyBytes.length, 32));
        return new SecretKeySpec(key, "AES");
    }

    public String encrypt(String plaintext) {
        try {
            SecretKey secretKey = getSecretKey();
            Cipher cipher = Cipher.getInstance(AES_GCM_ALGORITHM);

            byte[] iv = new byte[GCM_IV_LENGTH];
            new SecureRandom().nextBytes(iv);

            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, parameterSpec);

            byte[] encryptedBytes = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

            byte[] encryptedBytesWithIv = new byte[GCM_IV_LENGTH + encryptedBytes.length];
            System.arraycopy(iv, 0, encryptedBytesWithIv, 0, GCM_IV_LENGTH);
            System.arraycopy(encryptedBytes, 0, encryptedBytesWithIv, GCM_IV_LENGTH, encryptedBytes.length);

            return Base64.getEncoder().encodeToString(encryptedBytesWithIv);
        } catch (Exception e) {
            log.error("加密失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "加密失败", e);
        }
    }

    public String decrypt(String ciphertext) {
        try {
            SecretKey secretKey = getSecretKey();
            Cipher cipher = Cipher.getInstance(AES_GCM_ALGORITHM);

            byte[] encryptedBytesWithIv = Base64.getDecoder().decode(ciphertext);

            byte[] iv = new byte[GCM_IV_LENGTH];
            System.arraycopy(encryptedBytesWithIv, 0, iv, 0, GCM_IV_LENGTH);

            byte[] encryptedBytes = new byte[encryptedBytesWithIv.length - GCM_IV_LENGTH];
            System.arraycopy(encryptedBytesWithIv, GCM_IV_LENGTH, encryptedBytes, 0, encryptedBytes.length);

            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, parameterSpec);

            byte[] decryptedBytes = cipher.doFinal(encryptedBytes);
            return new String(decryptedBytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("解密失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "解密失败", e);
        }
    }

    public String encryptPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.isEmpty()) {
            return phoneNumber;
        }
        return encrypt(phoneNumber);
    }

    public String decryptPhoneNumber(String encryptedPhoneNumber) {
        if (encryptedPhoneNumber == null || encryptedPhoneNumber.isEmpty()) {
            return encryptedPhoneNumber;
        }
        return decrypt(encryptedPhoneNumber);
    }

    public String encryptEmail(String email) {
        if (email == null || email.isEmpty()) {
            return email;
        }
        return encrypt(email);
    }

    public String decryptEmail(String encryptedEmail) {
        if (encryptedEmail == null || encryptedEmail.isEmpty()) {
            return encryptedEmail;
        }
        return decrypt(encryptedEmail);
    }

    public String encryptAccessKey(String accessKey) {
        if (accessKey == null || accessKey.isEmpty()) {
            return accessKey;
        }
        return encrypt(accessKey);
    }

    public String decryptAccessKey(String encryptedAccessKey) {
        if (encryptedAccessKey == null || encryptedAccessKey.isEmpty()) {
            return encryptedAccessKey;
        }
        return decrypt(encryptedAccessKey);
    }
}
