package com.huicang.wise.application.common;

import lombok.Data;

@Data
public class DeleteWithCaptchaRequest {
    private Long id;
    private String captchaId;
    private String captchaCode;
}