package com.huicang.wise.common.log;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 服务器异常日志DTO
 *
 * @author WiseDepot
 * @version 1.0.0
 * @since 2026-03-09
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServerErrorLogDTO {

    /**
     * 异常堆栈信息
     */
    private String stackTrace;

    /**
     * 请求URL
     */
    private String requestUrl;

    /**
     * 请求方法
     */
    private String requestMethod;

    /**
     * 请求头
     */
    private Map<String, String> requestHeaders;

    /**
     * 请求体
     */
    private String requestBody;

    /**
     * 时间戳
     */
    private LocalDateTime timestamp;

    /**
     * 服务实例ID
     */
    private String serviceInstanceId;
}
