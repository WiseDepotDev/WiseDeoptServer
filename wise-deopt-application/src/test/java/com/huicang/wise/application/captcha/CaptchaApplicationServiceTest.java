package com.huicang.wise.application.captcha;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.huicang.wise.common.api.ErrorCode;
import com.huicang.wise.common.exception.BusinessException;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

/**
 * 验证码应用服务的单元测试：强制校验的"缺失即失败"语义、开关关闭时的跳过、生成与存储、 单次可用（成功后立即删除）与大小写不敏感比较。
 *
 * <p>安全语义逐条钉住： ① {@code enforceCaptcha} 把**缺失参数视为失败**（这正是注释里记录的"修复前不传字段即可跳过"的护栏）； ② {@code
 * wise.captcha.required=false} 会让所有入口**直接跳过**校验（配置即安全开关，必须显式测到）； ③ 校验成功后**立即删除** Redis 键 ⇒
 * 验证码**一次性**（防重放）； ④ 比较**忽略大小写**，且键值不过期时才能通过。
 */
@ExtendWith(MockitoExtension.class)
class CaptchaApplicationServiceTest {

    private static final String CAPTCHA_ID = "cid-1";
    private static final String REDIS_KEY = "captcha:" + CAPTCHA_ID;

    @Mock private StringRedisTemplate stringRedisTemplate;
    @Mock private ValueOperations<String, String> valueOperations;

    private CaptchaApplicationService service(boolean required) {
        return new CaptchaApplicationService(stringRedisTemplate, new ObjectMapper(), required);
    }

    // ---------------- 强制校验：缺失即失败 ----------------

    @Test
    @DisplayName("强制校验：captchaId 为空抛 VAL_PARAM_AUTH_CAPTCHA_ID_EMPTY（不传字段不能跳过）")
    void enforceRejectsBlankId() {
        CaptchaApplicationService service = service(true);

        assertEquals(
                ErrorCode.VAL_PARAM_AUTH_CAPTCHA_ID_EMPTY,
                assertThrows(BusinessException.class, () -> service.enforceCaptcha(null, "1234"))
                        .getErrorCode());
        assertEquals(
                ErrorCode.VAL_PARAM_AUTH_CAPTCHA_ID_EMPTY,
                assertThrows(BusinessException.class, () -> service.enforceCaptcha("  ", "1234"))
                        .getErrorCode());
        verifyNoInteractions(stringRedisTemplate);
    }

    @Test
    @DisplayName("强制校验：captchaCode 为空抛 VAL_PARAM_AUTH_CAPTCHA_CODE_EMPTY")
    void enforceRejectsBlankCode() {
        CaptchaApplicationService service = service(true);

        assertEquals(
                ErrorCode.VAL_PARAM_AUTH_CAPTCHA_CODE_EMPTY,
                assertThrows(
                                BusinessException.class,
                                () -> service.enforceCaptcha(CAPTCHA_ID, null))
                        .getErrorCode());
        assertEquals(
                ErrorCode.VAL_PARAM_AUTH_CAPTCHA_CODE_EMPTY,
                assertThrows(BusinessException.class, () -> service.enforceCaptcha(CAPTCHA_ID, ""))
                        .getErrorCode());
    }

    @Test
    @DisplayName("强制校验：开关关闭时直接跳过，不触达 Redis")
    void enforceSkippedWhenDisabled() {
        service(false).enforceCaptcha(null, null);

        verifyNoInteractions(stringRedisTemplate);
    }

    @Test
    @DisplayName("强制校验：验证码错误抛 PARAM_ERROR")
    void enforceRejectsWrongCode() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(REDIS_KEY)).thenReturn("ABCD");

        assertEquals(
                ErrorCode.PARAM_ERROR,
                assertThrows(
                                BusinessException.class,
                                () -> service(true).enforceCaptcha(CAPTCHA_ID, "ZZZZ"))
                        .getErrorCode());
    }

    @Test
    @DisplayName("强制校验：通过后立即删除键（一次性）")
    void enforceDeletesKeyOnSuccess() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(REDIS_KEY)).thenReturn("ABCD");

        service(true).enforceCaptcha(CAPTCHA_ID, "abcd");

        verify(stringRedisTemplate).delete(REDIS_KEY);
    }

    // ---------------- 生成 ----------------

    @Test
    @DisplayName("生成验证码：返回 ID、PNG data URL 与约 5 分钟后的过期时间")
    void generateCaptchaReturnsDto() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        LocalDateTime before = LocalDateTime.now();

        CaptchaDTO dto = service(true).generateCaptcha(new CaptchaGenerateRequest());

        assertNotNull(dto.getCaptchaId());
        assertFalse(dto.getCaptchaId().isBlank());
        assertTrue(dto.getCaptchaImage().startsWith("data:image/png;base64,"));
        assertTrue(dto.getExpireTime().isAfter(before), "过期时间应晚于当前时间，实际=" + dto.getExpireTime());
        assertTrue(dto.getExpireTime().isBefore(before.plusMinutes(6)));
    }

    @Test
    @DisplayName("生成验证码：以 captcha:<id> 为键写入 5 分钟 TTL")
    void generateCaptchaStoresCodeWithTtl() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);

        CaptchaDTO dto = service(true).generateCaptcha(new CaptchaGenerateRequest());

        ArgumentCaptor<String> codeCaptor = ArgumentCaptor.forClass(String.class);
        verify(valueOperations)
                .set(
                        eq("captcha:" + dto.getCaptchaId()),
                        codeCaptor.capture(),
                        eq(5L),
                        eq(TimeUnit.MINUTES));
        assertEquals(4, codeCaptor.getValue().length(), "验证码应为 4 位");
    }

    @Test
    @DisplayName("生成验证码：字符取自去混淆字符集（不含 I/L/O/0/1）")
    void generateCaptchaUsesUnambiguousCharacters() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);

        CaptchaDTO dto = service(true).generateCaptcha(new CaptchaGenerateRequest());

        ArgumentCaptor<String> codeCaptor = ArgumentCaptor.forClass(String.class);
        verify(valueOperations)
                .set(anyString(), codeCaptor.capture(), eq(5L), eq(TimeUnit.MINUTES));
        String code = codeCaptor.getValue();
        assertTrue(code.matches("^[A-Z2-9]{4}$"), "字符集不符，实际=" + code);
        assertFalse(code.matches(".*[ILO01].*"), "不应包含易混淆字符，实际=" + code);
    }

    // ---------------- 校验 ----------------

    @Test
    @DisplayName("校验：ID 或验证码为空抛 PARAM_ERROR")
    void verifyRejectsBlankFields() {
        CaptchaApplicationService service = service(true);
        CaptchaVerifyRequest noId = new CaptchaVerifyRequest();
        noId.setCaptchaCode("1234");
        assertEquals(
                ErrorCode.PARAM_ERROR,
                assertThrows(BusinessException.class, () -> service.verifyCaptcha(noId))
                        .getErrorCode());

        CaptchaVerifyRequest noCode = new CaptchaVerifyRequest();
        noCode.setCaptchaId(CAPTCHA_ID);
        assertEquals(
                ErrorCode.PARAM_ERROR,
                assertThrows(BusinessException.class, () -> service.verifyCaptcha(noCode))
                        .getErrorCode());
        verifyNoInteractions(stringRedisTemplate);
    }

    @Test
    @DisplayName("校验：键不存在（已过期）抛 PARAM_ERROR 且不删除")
    void verifyRejectsExpired() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(REDIS_KEY)).thenReturn(null);
        CaptchaVerifyRequest request = new CaptchaVerifyRequest();
        request.setCaptchaId(CAPTCHA_ID);
        request.setCaptchaCode("ABCD");

        assertEquals(
                ErrorCode.PARAM_ERROR,
                assertThrows(BusinessException.class, () -> service(true).verifyCaptcha(request))
                        .getErrorCode());
        verify(stringRedisTemplate, never()).delete(anyString());
    }

    @Test
    @DisplayName("校验：不匹配抛 PARAM_ERROR 且不删除（保留一次尝试机会）")
    void verifyRejectsMismatchWithoutDeleting() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(REDIS_KEY)).thenReturn("ABCD");
        CaptchaVerifyRequest request = new CaptchaVerifyRequest();
        request.setCaptchaId(CAPTCHA_ID);
        request.setCaptchaCode("ABCE");

        assertEquals(
                ErrorCode.PARAM_ERROR,
                assertThrows(BusinessException.class, () -> service(true).verifyCaptcha(request))
                        .getErrorCode());
        verify(stringRedisTemplate, never()).delete(anyString());
    }

    @Test
    @DisplayName("校验：大小写不敏感，通过后删除键（防重放）")
    void verifyIsCaseInsensitiveAndSingleUse() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(REDIS_KEY)).thenReturn("ABCD");
        CaptchaVerifyRequest request = new CaptchaVerifyRequest();
        request.setCaptchaId(CAPTCHA_ID);
        request.setCaptchaCode("abcd");

        service(true).verifyCaptcha(request);

        verify(stringRedisTemplate).delete(REDIS_KEY);
    }

    // ---------------- 布尔版校验 ----------------

    @Test
    @DisplayName("checkCaptcha：成功返回 true 并删除键")
    void checkCaptchaReturnsTrue() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(REDIS_KEY)).thenReturn("ABCD");

        assertTrue(service(true).checkCaptcha(CAPTCHA_ID, "ABCD"));
        verify(stringRedisTemplate).delete(REDIS_KEY);
    }

    @Test
    @DisplayName("checkCaptcha：失败返回 false 且不抛异常（登录入口的兼容路径）")
    void checkCaptchaReturnsFalseOnFailure() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(REDIS_KEY)).thenReturn("ABCD");

        assertFalse(service(true).checkCaptcha(CAPTCHA_ID, "ZZZZ"));
        verify(stringRedisTemplate, never()).delete(anyString());
    }

    @Test
    @DisplayName("checkCaptcha：参数为空同样返回 false（不抛异常）")
    void checkCaptchaReturnsFalseOnBlankParams() {
        assertFalse(service(true).checkCaptcha(null, null));
    }
}
