package com.huicang.wise.application.captcha;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.huicang.wise.common.api.ErrorCode;
import com.huicang.wise.common.exception.BusinessException;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class CaptchaApplicationService {

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    /**
     * 是否强制校验验证码。默认 true。 仅在单元测试/压测等受控场景下可通过 wise.captcha.required=false 关闭， 关闭后所有涉及验证码的入口都会跳过校验（会有
     * WARN 日志）。
     */
    private final boolean captchaRequired;

    private static final String CAPTCHA_PREFIX = "captcha:";
    private static final int CAPTCHA_EXPIRE_MINUTES = 5;
    private static final int CAPTCHA_LENGTH = 4;
    private static final int CAPTCHA_WIDTH = 120;
    private static final int CAPTCHA_HEIGHT = 40;

    private static final String[] CHARACTERS = {
        "A", "B", "C", "D", "E", "F", "G", "H", "J", "K", "M", "N", "P", "Q", "R", "S", "T", "U",
        "V", "W", "X", "Y", "Z", "2", "3", "4", "5", "6", "7", "8", "9"
    };

    public CaptchaApplicationService(
            StringRedisTemplate stringRedisTemplate,
            ObjectMapper objectMapper,
            @org.springframework.beans.factory.annotation.Value("${wise.captcha.required:true}")
                    boolean captchaRequired) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.objectMapper = objectMapper;
        this.captchaRequired = captchaRequired;
    }

    /**
     * 方法功能描述：强制校验验证码
     *
     * <p>与 {@link #checkCaptcha(String, String)} 的区别：本方法把"缺失参数"视为校验失败并抛出异常。 修复前登录等入口使用 {@code if
     * (captchaId != null && captchaCode != null)} 判断， 攻击者只要不传这两个字段即可完全跳过验证码校验。
     *
     * @param captchaId 验证码ID
     * @param captchaCode 用户填写的验证码
     * @throws BusinessException 参数缺失或验证码错误/过期
     */
    public void enforceCaptcha(String captchaId, String captchaCode) throws BusinessException {
        if (!captchaRequired) {
            log.warn("验证码校验已被配置关闭(wise.captcha.required=false)，当前请求跳过验证码校验");
            return;
        }
        if (captchaId == null || captchaId.isBlank()) {
            throw new BusinessException(
                    ErrorCode.VAL_PARAM_AUTH_CAPTCHA_ID_EMPTY,
                    ErrorCode.VAL_PARAM_AUTH_CAPTCHA_ID_EMPTY.getMessage());
        }
        if (captchaCode == null || captchaCode.isBlank()) {
            throw new BusinessException(
                    ErrorCode.VAL_PARAM_AUTH_CAPTCHA_CODE_EMPTY,
                    ErrorCode.VAL_PARAM_AUTH_CAPTCHA_CODE_EMPTY.getMessage());
        }
        verifyCaptcha(toVerifyRequest(captchaId, captchaCode));
    }

    private CaptchaVerifyRequest toVerifyRequest(String captchaId, String captchaCode) {
        CaptchaVerifyRequest request = new CaptchaVerifyRequest();
        request.setCaptchaId(captchaId);
        request.setCaptchaCode(captchaCode);
        return request;
    }

    public CaptchaDTO generateCaptcha(CaptchaGenerateRequest request) {
        String captchaId = UUID.randomUUID().toString();
        String captchaCode = generateCaptchaCode();
        String captchaImage = generateCaptchaImage(captchaCode);

        String redisKey = CAPTCHA_PREFIX + captchaId;
        stringRedisTemplate
                .opsForValue()
                .set(redisKey, captchaCode, CAPTCHA_EXPIRE_MINUTES, TimeUnit.MINUTES);

        CaptchaDTO dto = new CaptchaDTO();
        dto.setCaptchaId(captchaId);
        dto.setCaptchaImage(captchaImage);
        dto.setExpireTime(java.time.LocalDateTime.now().plusMinutes(CAPTCHA_EXPIRE_MINUTES));

        log.info("生成验证码成功，ID: {}, 过期时间: {}分钟", captchaId, CAPTCHA_EXPIRE_MINUTES);

        return dto;
    }

    public void verifyCaptcha(CaptchaVerifyRequest request) throws BusinessException {
        if (request.getCaptchaId() == null || request.getCaptchaId().isBlank()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "验证码ID不能为空");
        }

        if (request.getCaptchaCode() == null || request.getCaptchaCode().isBlank()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "验证码不能为空");
        }

        String redisKey = CAPTCHA_PREFIX + request.getCaptchaId();
        String storedCode = stringRedisTemplate.opsForValue().get(redisKey);

        if (storedCode == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "验证码已过期或不存在");
        }

        if (!storedCode.equalsIgnoreCase(request.getCaptchaCode())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "验证码错误");
        }

        stringRedisTemplate.delete(redisKey);

        log.info("验证码验证成功，ID: {}", request.getCaptchaId());
    }

    public boolean checkCaptcha(String captchaId, String captchaCode) {
        try {
            verifyCaptcha(toVerifyRequest(captchaId, captchaCode));
            return true;
        } catch (BusinessException e) {
            log.warn("验证码验证失败: {}", e.getMessage());
            return false;
        }
    }

    private String generateCaptchaCode() {
        Random random = new Random();
        StringBuilder code = new StringBuilder();
        for (int i = 0; i < CAPTCHA_LENGTH; i++) {
            code.append(CHARACTERS[random.nextInt(CHARACTERS.length)]);
        }
        return code.toString();
    }

    private String generateCaptchaImage(String code) {
        BufferedImage image =
                new BufferedImage(CAPTCHA_WIDTH, CAPTCHA_HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();

        graphics.setColor(Color.WHITE);
        graphics.fillRect(0, 0, CAPTCHA_WIDTH, CAPTCHA_HEIGHT);

        Random random = new Random();

        for (int i = 0; i < code.length(); i++) {
            graphics.setColor(
                    new Color(random.nextInt(150), random.nextInt(150), random.nextInt(150)));
            Font font = new Font("Arial", Font.BOLD, 28);
            graphics.setFont(font);

            graphics.drawString(String.valueOf(code.charAt(i)), 20 + i * 25, 30);
        }

        for (int i = 0; i < 6; i++) {
            graphics.setColor(
                    new Color(random.nextInt(255), random.nextInt(255), random.nextInt(255)));
            graphics.drawLine(
                    random.nextInt(CAPTCHA_WIDTH),
                    random.nextInt(CAPTCHA_HEIGHT),
                    random.nextInt(CAPTCHA_WIDTH),
                    random.nextInt(CAPTCHA_HEIGHT));
        }

        for (int i = 0; i < 30; i++) {
            graphics.setColor(
                    new Color(random.nextInt(255), random.nextInt(255), random.nextInt(255)));
            graphics.fillOval(random.nextInt(CAPTCHA_WIDTH), random.nextInt(CAPTCHA_HEIGHT), 2, 2);
        }

        graphics.dispose();

        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            javax.imageio.ImageIO.write(image, "png", baos);
            byte[] imageBytes = baos.toByteArray();
            return "data:image/png;base64," + Base64.getEncoder().encodeToString(imageBytes);
        } catch (Exception e) {
            log.error("生成验证码图片失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "生成验证码失败");
        }
    }
}
