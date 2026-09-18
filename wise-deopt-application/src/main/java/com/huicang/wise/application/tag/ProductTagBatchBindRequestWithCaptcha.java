package com.huicang.wise.application.tag;

import java.util.List;
import lombok.Data;

@Data
public class ProductTagBatchBindRequestWithCaptcha {
    private Long productId;
    private List<Long> tagIds;
    private String captchaId;
    private String captchaCode;
}
