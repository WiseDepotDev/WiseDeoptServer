package com.huicang.wise.infrastructure.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.Map;
import java.util.TreeMap;

@Component
@Slf4j
public class RequestSignatureService {

    @Value("${api.signature.secret:wise-depot-api-signature-secret-key-2024}")
    private String signatureSecret;

    private static final String HMAC_SHA256_ALGORITHM = "HmacSHA256";
    private static final long TIMESTAMP_TOLERANCE_SECONDS = 300;

    public String generateSignature(String method, String uri, Map<String, String> params, String timestamp, String nonce) {
        try {
            TreeMap<String, String> sortedParams = new TreeMap<>(params);
            sortedParams.put("timestamp", timestamp);
            sortedParams.put("nonce", nonce);

            StringBuilder queryString = new StringBuilder();
            for (Map.Entry<String, String> entry : sortedParams.entrySet()) {
                if (queryString.length() > 0) {
                    queryString.append("&");
                }
                queryString.append(entry.getKey()).append("=").append(entry.getValue());
            }

            String stringToSign = method.toUpperCase() + "\n" +
                    uri + "\n" +
                    queryString.toString();

            log.info("Generating signature - String to sign: {}", stringToSign);

            Mac mac = Mac.getInstance(HMAC_SHA256_ALGORITHM);
            SecretKeySpec secretKeySpec = new SecretKeySpec(signatureSecret.getBytes(StandardCharsets.UTF_8), HMAC_SHA256_ALGORITHM);
            mac.init(secretKeySpec);

            byte[] signatureBytes = mac.doFinal(stringToSign.getBytes(StandardCharsets.UTF_8));
            String signature = Base64.getEncoder().encodeToString(signatureBytes);
            
            log.info("Generated signature: {}", signature);
            
            return signature;
        } catch (Exception e) {
            log.error("生成签名失败", e);
            throw new RuntimeException("生成签名失败", e);
        }
    }

    public boolean verifySignature(String method, String uri, Map<String, String> params, String timestamp, String nonce, String signature) {
        try {
            String expectedSignature = generateSignature(method, uri, params, timestamp, nonce);
            return MessageDigest.isEqual(signature.getBytes(StandardCharsets.UTF_8), expectedSignature.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            log.error("验证签名失败", e);
            return false;
        }
    }

    public boolean validateTimestamp(String timestamp) {
        try {
            long requestTimestamp = Long.parseLong(timestamp);
            long currentTime = System.currentTimeMillis();
            
            long diff;
            if (requestTimestamp < 1000000000000L) {
                diff = Math.abs(currentTime / 1000 - requestTimestamp);
            } else {
                diff = Math.abs(currentTime - requestTimestamp);
            }
            
            return diff <= TIMESTAMP_TOLERANCE_SECONDS * 1000;
        } catch (NumberFormatException e) {
            log.error("时间戳格式错误: {}", timestamp);
            return false;
        }
    }
}
