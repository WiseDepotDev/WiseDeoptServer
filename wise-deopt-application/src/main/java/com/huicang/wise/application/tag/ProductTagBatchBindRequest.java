package com.huicang.wise.application.tag;

import java.util.List;

/**
 * 类功能描述：批量绑定标签请求
 *
 * @author xingchentye
 * @date 2026-02-27
 */
public class ProductTagBatchBindRequest {

    private Long productId;

    private List<Long> tagIds;

    /** 人机验证票据（一次性，由桥按用途注入）。缺失即失败。 */
    private String humanToken;

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public List<Long> getTagIds() {
        return tagIds;
    }

    public void setTagIds(List<Long> tagIds) {
        this.tagIds = tagIds;
    }

    public String getHumanToken() {
        return humanToken;
    }

    public void setHumanToken(String humanToken) {
        this.humanToken = humanToken;
    }
}
