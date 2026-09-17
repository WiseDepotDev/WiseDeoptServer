package com.huicang.wise.application.captcha;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CaptchaDTO {
    private String captchaId;
    private String captchaImage;
    private LocalDateTime expireTime;
}