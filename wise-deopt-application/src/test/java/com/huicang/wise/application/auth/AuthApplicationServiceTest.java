package com.huicang.wise.application.auth;

import com.huicang.wise.common.exception.BusinessException;
import com.huicang.wise.infrastructure.repository.user.UserCoreJpaEntity;
import com.huicang.wise.infrastructure.repository.user.UserCoreRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthApplicationServiceTest {

    @Mock
    private UserCoreRepository userCoreRepository;

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private AuthApplicationService authApplicationService;

    @BeforeEach
    void setUp() {
        // leniency allows stubbing to be ignored if not used (e.g. in exception tests)
        lenient().when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    private String hash(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encodedhash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder(2 * encodedhash.length);
            for (byte b : encodedhash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void testNfcPinLoginSuccess() {
        String nfcId = "test-nfc";
        String pin = "123456";
        String pinHash = hash(pin);

        UserCoreJpaEntity user = new UserCoreJpaEntity();
        user.setUsername("testuser");
        user.setNfcId(nfcId);
        user.setPinHash(pinHash);
        user.setEnabled(true);
        user.setLoginFailCount(0);

        when(userCoreRepository.findByNfcId(nfcId)).thenReturn(Optional.of(user));

        NfcPinDTO request = new NfcPinDTO();
        request.setNfcId(nfcId);
        request.setPin(pin);

        LoginResponse response = authApplicationService.nfcPinLogin(request);

        assertNotNull(response);
        assertEquals("testuser", response.getUsername());
        assertNotNull(response.getAccessToken());

        // Verify failure count reset
        verify(userCoreRepository, times(1)).save(user);
        assertEquals(0, user.getLoginFailCount());
    }

    @Test
    void testNfcPinLoginWrongPin() {
        String nfcId = "test-nfc";
        String pin = "123456";
        String wrongPin = "654321";
        String pinHash = hash(pin);

        UserCoreJpaEntity user = new UserCoreJpaEntity();
        user.setUsername("testuser");
        user.setNfcId(nfcId);
        user.setPinHash(pinHash);
        user.setEnabled(true);
        user.setLoginFailCount(0);

        when(userCoreRepository.findByNfcId(nfcId)).thenReturn(Optional.of(user));

        NfcPinDTO request = new NfcPinDTO();
        request.setNfcId(nfcId);
        request.setPin(wrongPin);

        assertThrows(BusinessException.class, () -> authApplicationService.nfcPinLogin(request));

        // Verify failure count increment
        verify(userCoreRepository, times(1)).save(user);
        assertEquals(1, user.getLoginFailCount());
    }

    @Test
    void testNfcPinLoginLockout() {
        String nfcId = "test-nfc";
        String pin = "123456";
        String wrongPin = "654321";
        String pinHash = hash(pin);

        UserCoreJpaEntity user = new UserCoreJpaEntity();
        user.setUsername("testuser");
        user.setNfcId(nfcId);
        user.setPinHash(pinHash);
        user.setEnabled(true);
        user.setLoginFailCount(4); // One more fail to lock

        when(userCoreRepository.findByNfcId(nfcId)).thenReturn(Optional.of(user));

        NfcPinDTO request = new NfcPinDTO();
        request.setNfcId(nfcId);
        request.setPin(wrongPin);

        assertThrows(BusinessException.class, () -> authApplicationService.nfcPinLogin(request));

        // Verify locked
        verify(userCoreRepository, times(1)).save(user);
        assertEquals(5, user.getLoginFailCount());
        assertNotNull(user.getLockedUntil());
        assertTrue(user.getLockedUntil().isAfter(LocalDateTime.now()));
    }

    @Test
    void testNfcPinLoginAccountLocked() {
        String nfcId = "test-nfc";
        String pin = "123456";

        UserCoreJpaEntity user = new UserCoreJpaEntity();
        user.setNfcId(nfcId);
        user.setEnabled(true);
        user.setLockedUntil(LocalDateTime.now().plusMinutes(10));

        when(userCoreRepository.findByNfcId(nfcId)).thenReturn(Optional.of(user));

        NfcPinDTO request = new NfcPinDTO();
        request.setNfcId(nfcId);
        request.setPin(pin);

        assertThrows(BusinessException.class, () -> authApplicationService.nfcPinLogin(request));
        
        // Should not verify pin if already locked
        verify(userCoreRepository, never()).save(user);
    }
}
