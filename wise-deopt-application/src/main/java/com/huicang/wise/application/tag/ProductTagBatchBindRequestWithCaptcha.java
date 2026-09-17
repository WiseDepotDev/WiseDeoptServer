package com.huicang.wise.application.tag;

import lombok.Data;

import java.util.List;

@Data
public class ProductTagBatchBindRequestWithCaptcha {
    private Long productId;
    private List<Long> tagIds;
    private String captchaId;
    private String captchaCode;
}