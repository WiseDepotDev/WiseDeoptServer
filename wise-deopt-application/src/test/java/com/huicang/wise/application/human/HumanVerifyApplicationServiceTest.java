package com.huicang.wise.application.human;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.huicang.wise.common.api.ErrorCode;
import com.huicang.wise.common.exception.BusinessException;
import com.huicang.wise.infrastructure.security.LoginAttemptGuard;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.Signature;
import java.security.spec.ECGenParameterSpec;
import java.time.Duration;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

/**
 * 服务端人机验证的安全语义测试。
 *
 * <p>钉的是"**缺失即失败**"这一类语义 —— 它们不是功能，是边界：
 *
 * <ol>
 *   <li>票据缺失 ⇒ 必须失败（图形验证码时代在这里漏过一次，见 {@code deploy/captcha_fix_verify.py}）；
 *   <li>票据重放（第二次用）⇒ 失败；
 *   <li>票据跨用途（登录签发的拿去删用户）⇒ 失败；
 *   <li>签名不符 / 挑战过期 / 计算量证明不达标 ⇒ 失败；
 *   <li>挑战**无论成败都被消费**（防"拿同一个挑战反复试签名"）。
 * </ol>
 */
@ExtendWith(MockitoExtension.class)
class HumanVerifyApplicationServiceTest {

    private static final String CHALLENGE_ID = "ch-1";

    @Mock private StringRedisTemplate stringRedisTemplate;
    @Mock private ValueOperations<String, String> valueOperations;
    @Mock private LoginAttemptGuard loginAttemptGuard;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /** 一把真的 P-256 密钥：签名必须真的能用，测试才有意义。 */
    private KeyPair keyPair;

    private String publicKeyHex;

    /**
     * 设备指纹**必须是公钥的哈希前缀**（服务端会重算并比对）。
     *
     * <p>这里不能写死一个常量：第一次写这版测试时就是写死的，于是"签名正确"的用例全部失败在 {@code HUMAN_SIGNATURE_INVALID} ——
     * 那个失败本身是对的，它证明了服务端确实在核对指纹， 而不是"随便给个 keyId 就认"。
     */
    private String deviceKeyId;

    @BeforeEach
    void setUp() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("EC");
        generator.initialize(new ECGenParameterSpec("secp256r1"));
        keyPair = generator.generateKeyPair();
        publicKeyHex = HexFormat.of().formatHex(uncompressed(keyPair));
        deviceKeyId = sha256Hex(uncompressed(keyPair)).substring(0, 32);
        lenient().when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    private HumanVerifyApplicationService service(boolean enforce) {
        return new HumanVerifyApplicationService(
                stringRedisTemplate, objectMapper, loginAttemptGuard, enforce);
    }

    /** 把公钥编码成未压缩点（04 ‖ X ‖ Y，65 字节）——与客户端同一口径。 */
    private static byte[] uncompressed(KeyPair pair) {
        java.security.interfaces.ECPublicKey publicKey =
                (java.security.interfaces.ECPublicKey) pair.getPublic();
        byte[] x = toFixed(publicKey.getW().getAffineX().toByteArray(), 32);
        byte[] y = toFixed(publicKey.getW().getAffineY().toByteArray(), 32);
        byte[] out = new byte[65];
        out[0] = 0x04;
        System.arraycopy(x, 0, out, 1, 32);
        System.arraycopy(y, 0, out, 33, 32);
        return out;
    }

    private static byte[] toFixed(byte[] value, int length) {
        if (value.length == length) {
            return value;
        }
        byte[] out = new byte[length];
        int copy = Math.min(value.length, length);
        System.arraycopy(value, value.length - copy, out, length - copy, copy);
        return out;
    }

    private String sha256Hex(byte[] input) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(input));
    }

    private String sign(String text) throws Exception {
        Signature signer = Signature.getInstance("SHA256withECDSA");
        signer.initSign(keyPair.getPrivate());
        signer.update(text.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(signer.sign());
    }

    private HumanVerifyRequest request(String powNonce, String signature, String purpose) {
        HumanVerifyRequest request = new HumanVerifyRequest();
        request.setChallengeId(CHALLENGE_ID);
        request.setPurpose(purpose);
        request.setPowNonce(powNonce);
        request.setDeviceKeyId(deviceKeyId);
        request.setDevicePublicKey(publicKeyHex);
        request.setSignature(signature);
        return request;
    }

    private void givenChallenge(int bits, String purpose) throws Exception {
        when(valueOperations.get("human:challenge:" + CHALLENGE_ID))
                .thenReturn(
                        objectMapper.writeValueAsString(
                                Map.of("purpose", purpose, "difficultyBits", bits)));
    }

    /** 供测试用的正式签名（原文与生产实现逐字一致）。 */
    private String signedText(String purpose, String nonce) {
        return CHALLENGE_ID + "|" + purpose + "|" + nonce + "|" + deviceKeyId;
    }

    /** 找出一个满足难度要求的 nonce（测试自己算，不依赖被测算的代码）。 */
    private String solvePow(int bits) throws Exception {
        for (int i = 0; i < 1_000_000; i++) {
            String nonce = "n" + i;
            String hex =
                    sha256Hex(
                            (CHALLENGE_ID + "|" + nonce + "|" + deviceKeyId)
                                    .getBytes(StandardCharsets.UTF_8));
            if (leadingZeroBits(HexFormat.of().parseHex(hex)) >= bits) {
                return nonce;
            }
        }
        throw new IllegalStateException("难度过高，测试算不出来");
    }

    /** 测试侧独立的"前导零 bit"实现（**故意不复用被测代码**：复用它等于自己给自己判卷）。 */
    private static int leadingZeroBits(byte[] digest) {
        int bits = 0;
        for (byte b : digest) {
            if (b == 0) {
                bits += 8;
            } else {
                return bits + Integer.numberOfLeadingZeros(b & 0xFF) - 24;
            }
        }
        return bits;
    }

    // ---------------------------------------------------------------- 放行判据

    @Test
    @DisplayName("票据缺失必须失败（不许写成「没传就跳过」——那是图形验证码时代踩过的洞）")
    void enforceRejectsMissingToken() {
        HumanVerifyApplicationService service = service(true);

        assertEquals(
                ErrorCode.HUMAN_TOKEN_REQUIRED,
                assertThrows(
                                BusinessException.class,
                                () -> service.enforce(null, HumanPurpose.USER_DELETE))
                        .getErrorCode());
        assertEquals(
                ErrorCode.HUMAN_TOKEN_REQUIRED,
                assertThrows(
                                BusinessException.class,
                                () -> service.enforce("   ", HumanPurpose.USER_DELETE))
                        .getErrorCode());
    }

    @Test
    @DisplayName("票据过期/不存在 ⇒ 失败")
    void enforceRejectsUnknownToken() {
        when(valueOperations.get("human:token:t-1")).thenReturn(null);
        HumanVerifyApplicationService service = service(true);

        assertEquals(
                ErrorCode.HUMAN_TOKEN_INVALID,
                assertThrows(
                                BusinessException.class,
                                () -> service.enforce("t-1", HumanPurpose.USER_DELETE))
                        .getErrorCode());
    }

    @Test
    @DisplayName("票据跨用途使用必须失败（登录换来的票据不能拿去删用户）")
    void enforceRejectsPurposeMismatch() throws Exception {
        when(valueOperations.get("human:token:t-2"))
                .thenReturn(objectMapper.writeValueAsString(Map.of("purpose", "LOGIN")));
        HumanVerifyApplicationService service = service(true);

        assertEquals(
                ErrorCode.HUMAN_TOKEN_INVALID,
                assertThrows(
                                BusinessException.class,
                                () -> service.enforce("t-2", HumanPurpose.USER_DELETE))
                        .getErrorCode());
    }

    @Test
    @DisplayName("票据用掉即删（同一张票第二次用必然失败）")
    void consumeDeletesToken() throws Exception {
        when(valueOperations.get("human:token:t-3"))
                .thenReturn(objectMapper.writeValueAsString(Map.of("purpose", "USER_DELETE")));
        HumanVerifyApplicationService service = service(true);

        service.enforce("t-3", HumanPurpose.USER_DELETE);
        verify(stringRedisTemplate).delete("human:token:t-3");
    }

    @Test
    @DisplayName("过渡期开关关闭时只记日志、不改行为（收口后这个分支会被删掉）")
    void enforceSkipsWhenDisabled() {
        service(false).enforce(null, HumanPurpose.USER_DELETE);
        // 不碰 Redis：连一次读都没有（"开关关掉"不等于"读不到就当过"）
        verify(valueOperations, never()).get(anyString());
    }

    // ---------------------------------------------------------------- 校验环节

    @Test
    @DisplayName("挑战不存在（过期或已用）⇒ HUMAN_CHALLENGE_EXPIRED")
    void verifyRejectsMissingChallenge() {
        when(valueOperations.get("human:challenge:" + CHALLENGE_ID)).thenReturn(null);

        assertEquals(
                ErrorCode.HUMAN_CHALLENGE_EXPIRED,
                assertThrows(
                                BusinessException.class,
                                () ->
                                        service(true)
                                                .verify(request("n0", "sig", "LOGIN"), "127.0.0.1"))
                        .getErrorCode());
    }

    @Test
    @DisplayName("签名不符 ⇒ HUMAN_SIGNATURE_INVALID，且挑战**已经被消费**（不能拿同一个挑战反复试）")
    void verifyRejectsBadSignatureAndStillConsumesChallenge() throws Exception {
        givenChallenge(0, "LOGIN");
        HumanVerifyApplicationService service = service(true);

        assertEquals(
                ErrorCode.HUMAN_SIGNATURE_INVALID,
                assertThrows(
                                BusinessException.class,
                                () -> service.verify(request("n0", "AAAA", "LOGIN"), "127.0.0.1"))
                        .getErrorCode());
        verify(stringRedisTemplate).delete("human:challenge:" + CHALLENGE_ID);
    }

    @Test
    @DisplayName("设备指纹与公钥不一致 ⇒ 失败（否则可以用 A 的指纹配 B 的签名）")
    void verifyRejectsKeyIdMismatch() throws Exception {
        givenChallenge(0, "LOGIN");
        HumanVerifyRequest request = request("n0", "AAAA", "LOGIN");
        request.setDeviceKeyId("ffffffffffffffffffffffffffffffff");

        assertEquals(
                ErrorCode.HUMAN_SIGNATURE_INVALID,
                assertThrows(
                                BusinessException.class,
                                () -> service(true).verify(request, "127.0.0.1"))
                        .getErrorCode());
    }

    @Test
    @DisplayName("计算量证明不达标 ⇒ HUMAN_POW_INSUFFICIENT")
    void verifyRejectsInsufficientPow() throws Exception {
        givenChallenge(12, "LOGIN");
        String nonce = "n0";
        String signature = sign(signedText("LOGIN", nonce));
        // 先确认这个 nonce 真的不达标（否则这条用例会在别的机器上变成假绿）
        String hex =
                sha256Hex(
                        (CHALLENGE_ID + "|" + nonce + "|" + deviceKeyId)
                                .getBytes(StandardCharsets.UTF_8));
        assertTrue(leadingZeroBits(HexFormat.of().parseHex(hex)) < 12, "这个 nonce 恰好达标了，换一个");

        assertEquals(
                ErrorCode.HUMAN_POW_INSUFFICIENT,
                assertThrows(
                                BusinessException.class,
                                () ->
                                        service(true)
                                                .verify(
                                                        request(nonce, signature, "LOGIN"),
                                                        "127.0.0.1"))
                        .getErrorCode());
    }

    @Test
    @DisplayName("签名 + 计算量证明都对 ⇒ 下发一次性票据，且挑战已被消费")
    void verifyIssuesToken() throws Exception {
        givenChallenge(12, "LOGIN");
        String nonce = solvePow(12);
        HumanVerifyApplicationService service = service(true);

        HumanVerifyResponse response =
                service.verify(
                        request(nonce, sign(signedText("LOGIN", nonce)), "LOGIN"), "127.0.0.1");

        assertNotNull(response.getHumanToken());
        assertEquals(120, response.getExpiresIn());
        verify(stringRedisTemplate).delete("human:challenge:" + CHALLENGE_ID);
        verify(valueOperations)
                .set(
                        eq("human:token:" + response.getHumanToken()),
                        anyString(),
                        eq(Duration.ofSeconds(120)));
    }

    @Test
    @DisplayName("挑战里的用途与提交的用途不一致 ⇒ 失败（跨用途提交）")
    void verifyRejectsPurposeMismatch() throws Exception {
        givenChallenge(0, "LOGIN");
        String nonce = "n0";

        assertEquals(
                ErrorCode.HUMAN_CHALLENGE_EXPIRED,
                assertThrows(
                                BusinessException.class,
                                () ->
                                        service(true)
                                                .verify(
                                                        request(
                                                                nonce,
                                                                sign(
                                                                        CHALLENGE_ID
                                                                                + "|USER_DELETE|"
                                                                                + nonce
                                                                                + "|"
                                                                                + deviceKeyId),
                                                                "USER_DELETE"),
                                                        "127.0.0.1"))
                        .getErrorCode());
    }

    @Test
    @DisplayName("用途认不出 ⇒ HUMAN_PURPOSE_INVALID（不猜、不默认放行）")
    void challengeRejectsUnknownPurpose() {
        HumanChallengeRequest request = new HumanChallengeRequest();
        request.setPurpose("WHATEVER");

        assertEquals(
                ErrorCode.HUMAN_PURPOSE_INVALID,
                assertThrows(
                                BusinessException.class,
                                () -> service(true).challenge(request, "127.0.0.1"))
                        .getErrorCode());
    }

    // ---------------------------------------------------------------- 风险规则

    @Test
    @DisplayName("R9 兜底：低风险 ⇒ 12 bit、action=verify")
    void challengeLowRisk() throws Exception {
        when(loginAttemptGuard.isLocked("u1")).thenReturn(false);
        when(loginAttemptGuard.isIpBlocked(anyString())).thenReturn(false);
        when(loginAttemptGuard.currentFailures("u1")).thenReturn(0);

        HumanChallengeResponse response = challengeFor("u1", null);

        assertEquals("verify", response.getAction());
        assertEquals(12, response.getDifficultyBits());
    }

    @Test
    @DisplayName("R1：失败 3 次以上 ⇒ 升到 18 bit（仍可过，只是更贵）")
    void challengeElevatesOnFailures() throws Exception {
        when(loginAttemptGuard.isLocked("u1")).thenReturn(false);
        when(loginAttemptGuard.isIpBlocked(anyString())).thenReturn(false);
        when(loginAttemptGuard.currentFailures("u1")).thenReturn(3);

        assertEquals(18, challengeFor("u1", null).getDifficultyBits());
    }

    @Test
    @DisplayName("R2：账号锁定 ⇒ 不给 verify，走冷却（不发票据）")
    void challengeLockedAccount() throws Exception {
        when(loginAttemptGuard.isLocked("u1")).thenReturn(true);

        HumanChallengeResponse response = challengeFor("u1", null);

        assertEquals("cooldown", response.getAction());
        assertEquals(3_000L, response.getRetryAfterMs());
    }

    @Test
    @DisplayName("R5/R6：环境异常或交互不足 ⇒ **照发挑战**、升到 22 bit（升级只提成本，不是拒绝）")
    void challengeElevatesOnDirtyEvidence() throws Exception {
        when(loginAttemptGuard.isLocked(any())).thenReturn(false);
        when(loginAttemptGuard.isIpBlocked(anyString())).thenReturn(false);

        HumanEvidence webdriver = new HumanEvidence();
        webdriver.setWebdriver(true);
        HumanChallengeResponse onWebdriver = challengeFor("u1", webdriver);
        assertEquals("verify", onWebdriver.getAction(), "环境异常不能被判成拒绝：图形码删了，没有第二条路");
        assertEquals(22, onWebdriver.getDifficultyBits(), "成本要真的提高：22 bit");

        HumanEvidence tooFewSamples = new HumanEvidence();
        tooFewSamples.setGestureSamples(2);
        HumanChallengeResponse onSamples = challengeFor("u1", tooFewSamples);
        assertEquals("verify", onSamples.getAction());
        assertEquals(22, onSamples.getDifficultyBits());
    }

    @Test
    @DisplayName("R6：采样点够多但**点与点之间是瞬移** ⇒ 也升到 22 bit（脚本补得出采样点，补不出轨迹）")
    void challengeElevatesOnTeleportTrack() throws Exception {
        when(loginAttemptGuard.isLocked(any())).thenReturn(false);
        when(loginAttemptGuard.isIpBlocked(anyString())).thenReturn(false);

        // 页面一直在采 maxJumpPx（见 apps/web/src/components/humanEvidence.ts）。
        // 这条用例钉的是"页面采了、服务端真的看得见"：DTO 里漏掉这个字段时，
        // Jackson 会把它丢掉，而**测试仍会全绿** —— 所以必须有一条用例走它。
        HumanEvidence teleport = new HumanEvidence();
        teleport.setGestureSamples(40);
        teleport.setMaxJumpPx(50_000);

        HumanChallengeResponse response = challengeFor("u1", teleport);
        assertEquals("verify", response.getAction());
        assertEquals(22, response.getDifficultyBits(), "瞬移轨迹要按高档收费");
    }

    @Test
    @DisplayName("R5/R6 读的是**不会变的事实**：同一个坏环境连问两次，第二次也必须还能过")
    void dirtyEvidenceNeverLocksOut() throws Exception {
        when(loginAttemptGuard.isLocked(any())).thenReturn(false);
        when(loginAttemptGuard.isIpBlocked(anyString())).thenReturn(false);

        // 未打包的 Electron（开发/便携版）会**如实**上报 shellPackaged=false。
        // 这条用例钉住"它不会因为这条事实被永久挡在门外"：
        // 老实现把它当"冷却重试"，而事实不会变 ⇒ 每次都是冷却 ⇒ 永远进不来。
        HumanEvidence unpackaged = new HumanEvidence();
        unpackaged.setShellPackaged(false);

        for (int i = 0; i < 3; i++) {
            HumanChallengeResponse response = challengeFor("u1", unpackaged);
            assertEquals(
                    "verify",
                    response.getAction(),
                    "第 " + (i + 1) + " 次仍必须是 verify（否则就是硬墙，而不是「贵一点」）");
        }
    }

    @Test
    @DisplayName("R8：上次计算量证明没算完 ⇒ 同一个 IP 的下一次挑战降回 12 bit，且只用一次")
    void powMissDowngradesNextChallenge() throws Exception {
        when(loginAttemptGuard.isLocked(any())).thenReturn(false);
        when(loginAttemptGuard.isIpBlocked(anyString())).thenReturn(false);
        // 第一次读到标记、之后读不到 —— 模拟"读到就删"。Mockito 的桩不会因为 delete 自己变，
        // 所以必须**按调用次数**给返回值，否则这条用例根本测不到"只用一次"。
        when(valueOperations.get("human:powmiss:127.0.0.1"))
                .thenReturn("{\"at\":1}")
                .thenReturn(null);

        // 环境还是那个坏环境（22 bit 的来源），但"已经付过一次成本"必须能收口
        HumanEvidence webdriver = new HumanEvidence();
        webdriver.setWebdriver(true);
        assertEquals(12, challengeFor("u1", webdriver).getDifficultyBits());

        // 标记被用掉了：再问一次又回到 22 bit（不能靠反复报空 nonce 换永久低难度）
        assertEquals(22, challengeFor("u1", webdriver).getDifficultyBits());
        verify(stringRedisTemplate).delete("human:powmiss:127.0.0.1");
    }

    @Test
    @DisplayName("计算量证明没算完时，服务端要**留下降档标记**（否则慢机器上的用户永远进不来）")
    void powMissLeavesDowngradeMark() throws Exception {
        givenChallenge(12, "LOGIN");
        String nonce = "n0";
        HumanVerifyApplicationService service = service(true);

        assertThrows(
                BusinessException.class,
                () ->
                        service.verify(
                                request(nonce, sign(signedText("LOGIN", nonce)), "LOGIN"),
                                "127.0.0.1"));

        verify(valueOperations)
                .set(eq("human:powmiss:127.0.0.1"), anyString(), eq(Duration.ofSeconds(300)));
    }

    private HumanChallengeResponse challengeFor(String username, HumanEvidence evidence) {
        HumanChallengeRequest request = new HumanChallengeRequest();
        request.setPurpose("LOGIN");
        request.setUsername(username);
        request.setPlatform("desktop");
        request.setEvidence(evidence);
        return service(true).challenge(request, "127.0.0.1");
    }

    @Test
    @DisplayName("挑战里存的是当时判定的难度（客户端不能拿低难度挑战去高风险时刻用）")
    void challengeStoresDifficulty() throws Exception {
        when(loginAttemptGuard.isLocked(any())).thenReturn(false);
        when(loginAttemptGuard.isIpBlocked(anyString())).thenReturn(false);
        when(loginAttemptGuard.currentFailures("u1")).thenReturn(9);

        HumanChallengeResponse response = challengeFor("u1", null);

        assertEquals(18, response.getDifficultyBits());
        verify(valueOperations)
                .set(
                        eq("human:challenge:" + response.getChallengeId()),
                        org.mockito.ArgumentMatchers.contains("\"difficultyBits\":18"),
                        eq(Duration.ofSeconds(120)));
    }
}
