package com.huicang.wise.application.human;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.huicang.wise.common.api.ErrorCode;
import com.huicang.wise.common.exception.BusinessException;
import com.huicang.wise.infrastructure.security.LoginAttemptGuard;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.ECParameterSpec;
import java.security.spec.ECPoint;
import java.security.spec.ECPublicKeySpec;
import java.time.Duration;
import java.util.Base64;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/**
 * 服务端人机验证：**这是唯一的放行边界**。
 *
 * <p>客户端那个"本地环境验证"（壳完整性、自动化特征、真人交互）只是**证据**，本服务可以完全不采信； 能放行的原因只有一条：对方在一次**新鲜的一次性挑战**上，用一把设备私钥签了字，
 * 并付出了服务端要求的计算量。
 *
 * <h2>为什么票据要绑用途、且用后即删</h2>
 *
 * 票据是"刚刚有人通过验证"的凭证。若不绑用途，一次登录验证换来的票据就能拿去删用户； 若不删，一张票据可以被重放到任意多次。两者都是这类设计最常见的洞，所以 {@link
 * #enforce(String, HumanPurpose)} 里"缺失/过期/已用/用途不符 ⇒ 抛异常"。
 *
 * <h2>为什么"缺失"不能是"跳过"（历史教训）</h2>
 *
 * 图形验证码时代踩过一次：{@code if (captchaId != null && captchaCode != null) { 校验 }} —— 攻击者不传这两个字段就完全绕过（见
 * {@code deploy/captcha_fix_verify.py}）。 所以这里的判据只能有一种写法：**先判空并抛错**，不存在"没传就跳过"的分支。
 *
 * <h2>风险规则（R1–R9，见方案 §7.3）</h2>
 *
 * 规则只做两件事：**加权（提高难度/冷却重试）** 与 **拒绝（锁定/封禁）**。 中间那一档给的是"机会"不是"惩罚"，因为图形验证码已经删掉了 —— 升级之后仍要有人能过。
 */
@Slf4j
@Service
public class HumanVerifyApplicationService {

    /** 挑战的 Redis 键前缀。 */
    private static final String CHALLENGE_PREFIX = "human:challenge:";

    /** 票据的 Redis 键前缀。 */
    private static final String TOKEN_PREFIX = "human:token:";

    /** 设备记录（首次见到的公钥与计数）的键前缀。 */
    private static final String DEVICE_PREFIX = "human:device:";

    /**
     * "这个 IP 上次没算完计算量证明"的键前缀（R8）。
     *
     * <p>为什么需要它：桥的时间预算很紧（它要保证"点一下就能过"，不肯为 PoW 卡住界面）。 一旦难度升到 22 bit，慢机器**必然算不完**；如果服务端只是回一个错误，
     * 那台机器上的用户就永远登不进来 —— 而图形码已经删了，"永远登不进来"没有任何替代出路。 所以：**没算完 = 已付过一次成本**，同一个 IP
     * 的下一次挑战降回低档，且这个降档只用一次。
     */
    private static final String POW_MISS_PREFIX = "human:powmiss:";

    /** 降档的保留时长（秒）。够一次重试用完，又不至于让"故意报空 nonce"长期换低难度。 */
    private static final int POW_MISS_TTL_SECONDS = 300;

    /** 挑战有效期（秒）。够短，才谈得上"新鲜"。 */
    private static final int CHALLENGE_TTL_SECONDS = 120;

    /** 票据有效期（秒）。 */
    private static final int TOKEN_TTL_SECONDS = 120;

    /** 低/中/高三档难度（需要命中的 SHA-256 前导零 bit）。 */
    private static final int BITS_LOW = 12;

    private static final int BITS_MID = 18;
    private static final int BITS_HIGH = 22;

    /** 高风险时的冷却时间（毫秒）：客户端等这么久再重试一次。 */
    private static final long COOLDOWN_MS = 3_000L;

    /** 时钟漂移容忍（毫秒）。超过就判可疑（R7）。 */
    private static final long CLOCK_SKEW_MS = 120_000L;

    /** 交互采样点的下限（R6）。低于它认为"这一下不是人点出来的"。 */
    private static final int MIN_GESTURE_SAMPLES = 8;

    /**
     * 相邻采样点跳变的上限（R6，像素）。
     *
     * <p>脚本可以补出很多采样点，但点是**瞬移**过去的。真人快速甩鼠标一帧也就几百像素， 超过这个数基本就是程序生成的轨迹。它只升难度、不拒绝 —— 高 DPI 大屏上甩一下也可能超。
     */
    private static final int MAX_JUMP_PX = 2_000;

    /** 账号连续失败多少次开始升档（R1）。 */
    private static final int FAILURES_FOR_MID = 3;

    public static final String ACTION_VERIFY = "verify";

    public static final String ACTION_COOLDOWN = "cooldown";

    private static final String RISK_LOW = "low";

    private static final String RISK_MID = "mid";

    private static final String RISK_HIGH = "high";

    private final StringRedisTemplate stringRedisTemplate;

    private final ObjectMapper objectMapper;

    private final LoginAttemptGuard loginAttemptGuard;

    /**
     * 开发期开关：{@code off} 时 {@link #enforce} 直接放行。
     *
     * <p><b>它不是"生产中关掉验证"的开关</b>：只用于"加新 → 切换 → 删旧"的过渡期， 收口（S5）时连同这个分支一起删掉。默认 off 保证 S1 上线时现网行为零变化。
     */
    private final boolean enforceEnabled;

    public HumanVerifyApplicationService(
            StringRedisTemplate stringRedisTemplate,
            ObjectMapper objectMapper,
            LoginAttemptGuard loginAttemptGuard,
            @Value("${wise.human-verify.enforce:true}") boolean enforceEnabled) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.objectMapper = objectMapper;
        this.loginAttemptGuard = loginAttemptGuard;
        this.enforceEnabled = enforceEnabled;
    }

    /** 一次判定的结果：把"难度/动作"与"为什么"分开，便于审计与测试。 */
    private record Decision(String action, int difficultyBits, String riskLevel, String reason) {}

    // ---------------------------------------------------------------- 挑战

    /**
     * 签发一次性挑战。
     *
     * <p>挑战里带上了当时的风险判定（难度），**校验时以挑战里的难度为准** —— 否则客户端可以先拿一个低难度挑战、再在高风险时刻提交它。
     */
    public HumanChallengeResponse challenge(HumanChallengeRequest request, String ip) {
        HumanPurpose purpose = HumanPurpose.parse(request == null ? null : request.getPurpose());
        if (purpose == null) {
            throw new BusinessException(ErrorCode.HUMAN_PURPOSE_INVALID);
        }

        String challengeId = UUID.randomUUID().toString();
        // 证据在**申请挑战**时就参与判定：难度随挑战签发，客户端才不会白算一次
        Decision decision = decide(purpose, request.getUsername(), ip, request.getEvidence());

        Map<String, Object> stored = new LinkedHashMap<>();
        stored.put("purpose", purpose.name());
        stored.put("difficultyBits", decision.difficultyBits());
        stored.put("ip", ip == null ? "" : ip);
        if (request.getUsername() != null) {
            stored.put("username", request.getUsername());
        }
        writeJson(CHALLENGE_PREFIX + challengeId, stored, CHALLENGE_TTL_SECONDS);

        log.info(
                "[human] 签发挑战 id={} purpose={} action={} bits={} risk={} 原因={}",
                challengeId,
                purpose,
                decision.action(),
                decision.difficultyBits(),
                decision.riskLevel(),
                decision.reason());

        HumanChallengeResponse response = new HumanChallengeResponse();
        response.setChallengeId(challengeId);
        response.setAction(decision.action());
        response.setDifficultyBits(decision.difficultyBits());
        response.setExpiresIn(CHALLENGE_TTL_SECONDS);
        response.setRetryAfterMs(ACTION_COOLDOWN.equals(decision.action()) ? COOLDOWN_MS : null);
        return response;
    }

    // ---------------------------------------------------------------- 校验

    /** 校验一次提交；通过则下发一次性票据。 */
    public HumanVerifyResponse verify(HumanVerifyRequest request, String ip) {
        if (request == null
                || request.getChallengeId() == null
                || request.getChallengeId().isBlank()) {
            throw new BusinessException(ErrorCode.HUMAN_CHALLENGE_REQUIRED);
        }
        HumanPurpose purpose = HumanPurpose.parse(request.getPurpose());
        if (purpose == null) {
            throw new BusinessException(ErrorCode.HUMAN_PURPOSE_INVALID);
        }

        /*
         * **先把挑战读出来并立刻删掉**（无论后面成败）。
         *
         * 顺序很讲究：如果先校验再删，一次错误的签名就不会消费挑战 —— 攻击者可以拿同一个挑战
         * 反复试签名/PoW（相当于把"一次性"变成了"无限次"）。所以先消费再校验。
         */
        String challengeKey = CHALLENGE_PREFIX + request.getChallengeId();
        String stored = stringRedisTemplate.opsForValue().get(challengeKey);
        if (stored == null) {
            throw new BusinessException(ErrorCode.HUMAN_CHALLENGE_EXPIRED);
        }
        stringRedisTemplate.delete(challengeKey);

        Map<?, ?> challenge = readJson(stored);
        String challengePurpose = String.valueOf(challenge.get("purpose"));
        if (!purpose.name().equals(challengePurpose)) {
            // 跨用途提交：挑战是给 A 签的，却拿去换 B 的票据
            log.warn("[human] 挑战用途不符：签发={} 提交={}", challengePurpose, purpose);
            throw new BusinessException(ErrorCode.HUMAN_CHALLENGE_EXPIRED);
        }
        int difficultyBits = toInt(challenge.get("difficultyBits"), BITS_LOW);

        verifySignature(request);
        verifyProofOfWork(
                request.getChallengeId(),
                request.getPowNonce(),
                request.getDeviceKeyId(),
                difficultyBits,
                ip);
        rememberDevice(request.getDeviceKeyId(), request.getDevicePublicKey());
        auditClockSkew(request);

        String token = UUID.randomUUID().toString().replace("-", "");
        Map<String, Object> tokenBody = new LinkedHashMap<>();
        tokenBody.put("purpose", purpose.name());
        tokenBody.put("deviceKeyId", request.getDeviceKeyId());
        writeJson(TOKEN_PREFIX + token, tokenBody, TOKEN_TTL_SECONDS);

        HumanVerifyResponse response = new HumanVerifyResponse();
        response.setHumanToken(token);
        response.setExpiresIn(TOKEN_TTL_SECONDS);
        response.setRiskLevel(difficultyBits >= BITS_MID ? RISK_MID : RISK_LOW);
        log.info(
                "[human] 验证通过 purpose={} bits={} device={}",
                purpose,
                difficultyBits,
                request.getDeviceKeyId());
        return response;
    }

    // ---------------------------------------------------------------- 放行

    /**
     * 业务入口的放行判据（登录 / 批量绑定 / 删除用户三处 Service 的第一行调用它）。
     *
     * <p>票据一次性、绑用途、限时。
     */
    public void enforce(String humanToken, HumanPurpose purpose) {
        if (!enforceEnabled) {
            // 过渡期：只记一条，不改行为。收口时这个分支与开关一起删除。
            log.debug("[human] enforce 处于过渡期（wise.human-verify.enforce=false），本次不校验");
            return;
        }
        if (humanToken == null || humanToken.isBlank()) {
            // 这里是那个历史洞的位置：**缺字段必须失败**，绝不能"没传就跳过"
            throw new BusinessException(ErrorCode.HUMAN_TOKEN_REQUIRED);
        }
        String key = TOKEN_PREFIX + humanToken;
        String stored = stringRedisTemplate.opsForValue().get(key);
        if (stored == null) {
            throw new BusinessException(ErrorCode.HUMAN_TOKEN_INVALID);
        }
        stringRedisTemplate.delete(key);
        Map<?, ?> body = readJson(stored);
        if (!purpose.name().equals(String.valueOf(body.get("purpose")))) {
            log.warn("[human] 票据跨用途使用：签发={} 使用={}", body.get("purpose"), purpose);
            throw new BusinessException(ErrorCode.HUMAN_TOKEN_INVALID);
        }
    }

    // ---------------------------------------------------------------- 风险规则

    /**
     * R1–R9 的落点：返回"动作 + 难度 + 原因"。
     *
     * <p>{@code evidence} 可以为 null（申请挑战时还没有证据）—— 此时只跑与证据无关的规则。
     */
    private Decision decide(
            HumanPurpose purpose, String username, String ip, HumanEvidence evidence) {
        // R2/R3：账号锁定与单 IP 封禁 —— 硬拒绝（不发挑战，也不给"再试一次"）
        if (purpose == HumanPurpose.LOGIN && username != null && !username.isBlank()) {
            if (loginAttemptGuard.isLocked(username)) {
                return new Decision(ACTION_COOLDOWN, BITS_HIGH, RISK_HIGH, "R2 账号已锁定");
            }
        }
        if (ip != null && !ip.isBlank() && loginAttemptGuard.isIpBlocked(ip)) {
            return new Decision(ACTION_COOLDOWN, BITS_HIGH, RISK_HIGH, "R3 单 IP 已封禁");
        }

        // R8：上一次的计算量证明没算完（这台机器慢，或难度当时是高档）——
        // **降回低档**并把标记用掉。它必须排在 R5/R6 前面：R5/R6 会升到 22 bit，
        // 而"已经付过一次成本、还是算不完"只有降档才能收口。否则慢机器上的用户
        // 会卡在"永远算不完 → 永远登不进来"，而图形码已经删了，没有第二条路。
        if (consumePowMiss(ip)) {
            return new Decision(ACTION_VERIFY, BITS_LOW, RISK_LOW, "R8 上次计算量证明未完成，本次降档");
        }

        // R5/R6：环境异常与交互不足 —— 升到高档，**但照发挑战**（不是拒绝：图形码已删，人必须能过）
        //
        // 这里为什么不再用"冷却"：冷却对**会变**的原因（账号失败次数）是"给一次机会"，
        // 可 R5/R6 读的是**不会变的事实**（是不是发布包、挂没挂调试器）—— 对同一个壳
        // 反复冷却等于永久拒绝，那就成了"因为你在开发环境所以永远登不进来"。
        // 升级成本的正确表达是"难度更高"，而不是"再审一次同样的证据"。
        if (evidence != null) {
            if (Boolean.TRUE.equals(evidence.getWebdriver())
                    || Boolean.TRUE.equals(evidence.getHeadless())
                    || Boolean.FALSE.equals(evidence.getShellPackaged())) {
                return new Decision(ACTION_VERIFY, BITS_HIGH, RISK_HIGH, "R5 环境异常");
            }
            Integer samples = evidence.getGestureSamples();
            if (samples != null && samples < MIN_GESTURE_SAMPLES) {
                return new Decision(ACTION_VERIFY, BITS_HIGH, RISK_HIGH, "R6 交互采样不足");
            }
            Integer maxJump = evidence.getMaxJumpPx();
            if (maxJump != null && maxJump > MAX_JUMP_PX) {
                return new Decision(ACTION_VERIFY, BITS_HIGH, RISK_HIGH, "R6 交互轨迹瞬移");
            }
        }

        // R7：时钟漂移
        // （只记审计，不在这里升难度 —— 见 auditClockSkew 的说明）

        // R1：登录失败次数 → 升档
        if (purpose == HumanPurpose.LOGIN
                && username != null
                && !username.isBlank()
                && loginAttemptGuard.currentFailures(username) >= FAILURES_FOR_MID) {
            return new Decision(ACTION_VERIFY, BITS_MID, RISK_MID, "R1 失败次数达阈值");
        }

        return new Decision(ACTION_VERIFY, BITS_LOW, RISK_LOW, "R9 兜底放行");
    }

    // ---------------------------------------------------------------- 密码学

    /**
     * 校验设备签名：{@code ECDSA-SHA256(challengeId|purpose|powNonce|deviceKeyId)}。
     *
     * <p>公钥从请求里来（**首用即信**）：它不授予任何权限，只用来证明"同一台设备在这次挑战上签了字"。 服务端仍然做两件事把成本抬起来：① 校验指纹与公钥一致 ② 校验点真的在
     * P-256 曲线上 （不在曲线上的点是"无效曲线攻击"的入口，直接拒）。
     */
    private void verifySignature(HumanVerifyRequest request) {
        String publicKeyHex = request.getDevicePublicKey();
        String signatureBase64 = request.getSignature();
        if (publicKeyHex == null
                || publicKeyHex.isBlank()
                || signatureBase64 == null
                || signatureBase64.isBlank()) {
            throw new BusinessException(ErrorCode.HUMAN_SIGNATURE_INVALID);
        }
        // ① 指纹必须与公钥一致（否则可以用 A 的指纹配 B 的签名）
        String expectedKeyId = sha256Hex(hexToBytes(publicKeyHex)).substring(0, 32);
        if (!expectedKeyId.equalsIgnoreCase(request.getDeviceKeyId())) {
            log.warn("[human] 设备指纹与公钥不一致");
            throw new BusinessException(ErrorCode.HUMAN_SIGNATURE_INVALID);
        }

        byte[] point = hexToBytes(publicKeyHex);
        if (point.length != 65 || point[0] != 0x04) {
            throw new BusinessException(ErrorCode.HUMAN_SIGNATURE_INVALID);
        }

        String signedText =
                request.getChallengeId()
                        + "|"
                        + request.getPurpose()
                        + "|"
                        + (request.getPowNonce() == null ? "" : request.getPowNonce())
                        + "|"
                        + request.getDeviceKeyId();

        try {
            PublicKey publicKey = publicKeyOf(point);
            Signature verifier = Signature.getInstance("SHA256withECDSA");
            verifier.initVerify(publicKey);
            verifier.update(signedText.getBytes(StandardCharsets.UTF_8));
            if (!verifier.verify(Base64.getDecoder().decode(signatureBase64))) {
                throw new BusinessException(ErrorCode.HUMAN_SIGNATURE_INVALID);
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.warn("[human] 签名校验异常：{}", e.getMessage());
            throw new BusinessException(ErrorCode.HUMAN_SIGNATURE_INVALID);
        }
    }

    /** 把 65 字节未压缩点变成 JCA 公钥，并**显式校验点在曲线上**。 */
    private PublicKey publicKeyOf(byte[] point) throws Exception {
        ECParameterSpec params = p256Parameters();
        BigInteger x = new BigInteger(1, java.util.Arrays.copyOfRange(point, 1, 33));
        BigInteger y = new BigInteger(1, java.util.Arrays.copyOfRange(point, 33, 65));
        // y² = x³ + ax + b (mod p)
        BigInteger p = ((java.security.spec.ECFieldFp) params.getCurve().getField()).getP();
        BigInteger lhs = y.modPow(BigInteger.TWO, p);
        BigInteger rhs =
                x.modPow(BigInteger.valueOf(3), p)
                        .add(params.getCurve().getA().multiply(x))
                        .add(params.getCurve().getB())
                        .mod(p);
        if (!lhs.equals(rhs)) {
            throw new BusinessException(ErrorCode.HUMAN_SIGNATURE_INVALID);
        }
        return KeyFactory.getInstance("EC")
                .generatePublic(new ECPublicKeySpec(new ECPoint(x, y), params));
    }

    private ECParameterSpec p256Parameters() throws Exception {
        java.security.AlgorithmParameters parameters =
                java.security.AlgorithmParameters.getInstance("EC");
        parameters.init(new ECGenParameterSpec("secp256r1"));
        return parameters.getParameterSpec(ECParameterSpec.class);
    }

    /**
     * 计算量证明：{@code SHA-256(challengeId|powNonce|deviceKeyId)} 需命中 {@code bits} 个前导零 bit。
     *
     * <p>它买到的只是"批量请求的边际成本"，**不是人类证明**。所以： 难度按风险给、上限 22 bit；客户端侧有 400ms 硬超时，超时由服务端降档重来。
     *
     * <p>没达标时除了报错，还要**给同一个 IP 留一次降档机会**（{@link #POW_MISS_PREFIX}）：
     * 客户端已经如实上报"算不完"，服务端若只说"不行"，慢机器上的用户就再也进不来了。
     */
    private void verifyProofOfWork(
            String challengeId, String powNonce, String deviceKeyId, int bits, String ip) {
        if (bits <= 0) {
            return;
        }
        if (powNonce == null || powNonce.isBlank()) {
            markPowMiss(ip);
            throw new BusinessException(ErrorCode.HUMAN_POW_INSUFFICIENT);
        }
        byte[] digest =
                sha256(
                        (challengeId + "|" + powNonce + "|" + deviceKeyId)
                                .getBytes(StandardCharsets.UTF_8));
        if (leadingZeroBits(digest) < bits) {
            markPowMiss(ip);
            throw new BusinessException(ErrorCode.HUMAN_POW_INSUFFICIENT);
        }
    }

    // ---------------------------------------------------------------- R8 降档标记

    /** 记下"这个 IP 这次没算完"，供**下一次**挑战降档。 */
    private void markPowMiss(String ip) {
        if (ip == null || ip.isBlank()) {
            return;
        }
        writeJson(
                POW_MISS_PREFIX + ip,
                Map.of("at", System.currentTimeMillis()),
                POW_MISS_TTL_SECONDS);
    }

    /**
     * 取用降档机会：**读到就删**，一次验证只换一次低难度（否则"报空 nonce"就成了永久免算）。
     *
     * <p>R8 标记是**优化提示**，不是安全状态：它只决定"这次发低档难度"。 所以读不到（Redis 不可用）时按"没有标记"处理即可 —— **不要**让它抛出去变成 500。
     * 2026-10-05 实测：Redis 停掉时这里先抛，界面拿到的是笼统的 `SYS-0001 系统异常`， 而真正该说的话（"人机验证状态不可用"）在后面那一步才说 ——
     * 报错信息指错了地方。
     */
    private boolean consumePowMiss(String ip) {
        if (ip == null || ip.isBlank()) {
            return false;
        }
        String key = POW_MISS_PREFIX + ip;
        try {
            if (stringRedisTemplate.opsForValue().get(key) == null) {
                return false;
            }
            stringRedisTemplate.delete(key);
        } catch (Exception e) {
            log.warn("[human] 读取降档标记失败（按「无标记」处理，不影响安全判定）: {}", e.getMessage());
            return false;
        }
        log.info("[human] R8 降档：IP {} 上次未完成计算量证明，本次按低档签发", ip);
        return true;
    }

    /**
     * R7：时钟漂移**只记审计，不因此升难度、也不因此拒绝**。
     *
     * <p>为什么与方案里的"升级到 18 bits"不同：升级需要把"这台设备时钟不准"记在设备档案上、 并在**下一次**签发挑战时读到它 —— 而设备标识（{@code
     * deviceKeyId}）在这一版里只有提交时才有。 硬要现在做，就会变成"因为时钟不准所以拒绝"，那是误伤（现场设备时钟不准是常态，
     * 手机没联网时尤其明显）。所以本版只记，升级留到客户端在申请挑战时带上 {@code deviceKeyId} 之后。
     */
    private void auditClockSkew(HumanVerifyRequest request) {
        Long clientTime = request.getClientTime();
        if (clientTime == null) {
            return;
        }
        long skew = Math.abs(System.currentTimeMillis() - clientTime);
        if (skew > CLOCK_SKEW_MS) {
            log.warn("[human] 时钟漂移 {}ms（device={}；只记审计）", skew, request.getDeviceKeyId());
        }
    }

    /** 首次见到的设备把公钥记下来；此后同一 keyId 必须用同一把公钥（防止 keyId 被冒用）。 */
    private void rememberDevice(String keyId, String publicKeyHex) {
        if (keyId == null || keyId.isBlank()) {
            return;
        }
        String key = DEVICE_PREFIX + keyId;
        String existing = stringRedisTemplate.opsForValue().get(key);
        if (existing == null) {
            writeJson(
                    key,
                    Map.of(
                            "publicKey",
                            publicKeyHex == null ? "" : publicKeyHex,
                            "firstSeen",
                            System.currentTimeMillis()),
                    (int) Duration.ofDays(30).toSeconds());
            return;
        }
        Map<?, ?> stored = readJson(existing);
        if (!String.valueOf(stored.get("publicKey")).equalsIgnoreCase(publicKeyHex)) {
            log.warn("[human] 同一 keyId 换了公钥：{}", keyId);
            throw new BusinessException(ErrorCode.HUMAN_SIGNATURE_INVALID);
        }
    }

    // ---------------------------------------------------------------- 小工具

    private void writeJson(String key, Map<String, Object> value, int ttlSeconds) {
        try {
            stringRedisTemplate
                    .opsForValue()
                    .set(
                            key,
                            objectMapper.writeValueAsString(value),
                            Duration.ofSeconds(ttlSeconds));
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "人机验证状态写入失败");
        }
    }

    private Map<?, ?> readJson(String text) {
        try {
            return objectMapper.readValue(text, Map.class);
        } catch (Exception e) {
            // 读不出来等同于"这个状态无效"：当作过期处理，不走"猜一猜继续"
            throw new BusinessException(ErrorCode.HUMAN_CHALLENGE_EXPIRED);
        }
    }

    private static int toInt(Object value, int fallback) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (RuntimeException e) {
            return fallback;
        }
    }

    private static byte[] hexToBytes(String hex) {
        try {
            return HexFormat.of().parseHex(hex.trim());
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.HUMAN_SIGNATURE_INVALID);
        }
    }

    private static byte[] sha256(byte[] input) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(input);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "SHA-256 不可用");
        }
    }

    private static String sha256Hex(byte[] input) {
        return HexFormat.of().formatHex(sha256(input));
    }

    /** 前导零 bit 数（256 位哈希）。 */
    static int leadingZeroBits(byte[] digest) {
        int bits = 0;
        for (byte b : digest) {
            if (b == 0) {
                bits += 8;
                continue;
            }
            bits += Integer.numberOfLeadingZeros(b & 0xFF) - 24;
            break;
        }
        return bits;
    }
}
