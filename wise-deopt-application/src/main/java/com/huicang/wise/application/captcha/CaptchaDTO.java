package com.huicang.wise.application.captcha;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class CaptchaDTO {
    private String captchaId;
    private String captchaImage;
    private LocalDateTime expireTime;
}
