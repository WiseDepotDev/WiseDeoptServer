package com.huicang.wise.common.api;

/**
 * 类功能描述：业务错误码枚举
 *
 * @author xingchentye
 * @date 2026-01-19
 * @modified xingchentye 2026-01-19 初始化通用错误码
 */
public enum ErrorCode {

    /**
     * 方法功能描述：成功
     */
    SUCCESS("RES-0000", "处理成功"),

    /**
     * 方法功能描述：通用请求参数错误
     */
    PARAM_ERROR("VAL-0001", "请求参数不合法"),

    /**
     * 方法功能描述：资源未找到
     */
    NOT_FOUND("RES-0004", "资源不存在"),

    /**
     * 方法功能描述：未认证或登录状态失效
     */
    UNAUTHORIZED("AUTH-0001", "未认证或登录状态已失效"),

    /**
     * 方法功能描述：无访问权限
     */
    FORBIDDEN("AUTH-0003", "无访问权限"),
    AUTH_USERNAME_EMPTY("VAL-PARAM-AUTH-1002", "username字段为空"),
    AUTH_PASSWORD_EMPTY("VAL-PARAM-AUTH-1004", "password字段为空"),
    AUTH_LOGIN_FAILED("AUTH-REQUEST-1002", "用户名或密码错误"),
    AUTH_ACCOUNT_DISABLED("AUTH-ACCOUNT-1001", "账户状态异常"),
    REQUEST_BODY_INVALID("VAL-REQUEST-1001", "请求体解析失败"),
    REQUEST_METHOD_NOT_ALLOWED("RES-REQUEST-1002", "请求方法不可用"),
    REQUEST_NOT_FOUND("RES-REQUEST-1001", "接口不存在"),
    REQUEST_ID_MISSING("RES-REQUEST-1004", "请求时未携带REQUEST-ID"),
    SYSTEM_REQUEST_ERROR("SYS-REQUEST-1001", "未知异常"),
    OSS_EXPIRES_OUT_OF_RANGE("VAL-RANGE-OSS-1001", "MinIO临时访问链接有效期超出范围"),
    OSS_SERVICE_UNAVAILABLE("SYS-IO-OSS-1001", "文件存储服务不可用或连接异常"),
    OSS_PRESIGN_ERROR("SYS-IO-OSS-1002", "获取MinIO临时访问链接异常"),
    OSS_STAT_ERROR("SYS-IO-OSS-1003", "获取MinIO文件状态异常"),
    OSS_UPLOAD_ERROR("SYS-IO-OSS-1004", "上传文件到MinIO失败"),
    OSS_DELETE_ERROR("SYS-IO-OSS-1005", "删除MinIO文件失败"),

    /**
     * 方法功能描述：系统内部错误
     */
    SYSTEM_ERROR("SYS-0001", "系统内部错误，请稍后重试");

    private final String code;

    private final String message;

    /**
     * 方法功能描述：枚举构造函数
     *
     * @param code    错误码
     * @param message 错误信息
     * @return 无
     */
    ErrorCode(String code, String message) {
        this.code = code;
        this.message = message;
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
