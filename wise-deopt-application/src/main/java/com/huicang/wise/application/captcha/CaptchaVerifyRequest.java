package com.huicang.wise.application.captcha;

import lombok.Data;

@Data
public class CaptchaVerifyRequest {
    private String captchaId;
    private String captchaCode;
}
