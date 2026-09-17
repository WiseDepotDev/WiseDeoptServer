package com.huicang.wise.common.api;

/**
 * 业务错误码枚举
 *
 * @author WiseDepot
 * @version 0.0.28
 * @since 2026-02-27
 */
public enum ErrorCode {

    SUCCESS("RES-0000", "处理成功", 200),

    PARAM_ERROR("VAL-0001", "请求参数不合法", 400),
    NOT_FOUND("RES-0004", "资源不存在", 404),

    UNAUTHORIZED("AUTH-0001", "未认证或登录状态已失效", 401),
    AUTH_INVALID_TOKEN("AUTH-0002", "无效的访问令牌", 401),
    AUTH_TOKEN_EXPIRED("AUTH-0003", "访问令牌已过期", 419),
    AUTH_REFRESH_TOKEN_INVALID("AUTH-0004", "刷新令牌已失效", 401),

    FORBIDDEN("AUTH-0005", "无访问权限", 403),

    SYSTEM_ERROR("SYS-0001", "系统内部错误，请稍后重试", 500),

    REQUEST_NOT_FOUND("RES-REQUEST-1001", "接口不存在", 404),
    REQUEST_METHOD_NOT_ALLOWED("RES-REQUEST-1002", "请求方法不可用", 405),
    REQUEST_ID_MISSING("RES-REQUEST-1004", "请求时未携带REQUEST-ID", 500),
    SYSTEM_REQUEST_ERROR("SYS-REQUEST-1001", "未知异常", 500),

    AUTH_REQUEST_NO_IP("AUTH-REQUEST-1001", "未检测到用户IP地址", 403),
    AUTH_REQUEST_USERNAME_PASSWORD_ERROR("AUTH-REQUEST-1002", "用户名或密码错误", 401),
    AUTH_REQUEST_EMAIL_PASSWORD_ERROR("AUTH-REQUEST-1003", "邮箱或密码错误", 401),
    AUTH_REQUEST_NFC_NOT_FOUND("AUTH-REQUEST-1004", "工牌不存在或已失效", 401),
    AUTH_REQUEST_VERIFICATION_ID_INVALID("AUTH-REQUEST-1005", "二步验证id无效或已过期", 401),
    AUTH_REQUEST_PIN_ERROR("AUTH-REQUEST-1006", "PIN码不匹配", 401),
    AUTH_REQUEST_REFRESH_TOKEN_INVALID("AUTH-REQUEST-1007", "刷新令牌已失效", 401),
    AUTH_REQUEST_PASSWORD_VERIFY_FAILED("AUTH-REQUEST-1008", "密码校验失败", 401),
    AUTH_REQUEST_TOKEN_PARSE_FAILED("AUTH-REQUEST-1009", "访问令牌解析失败", 401),
    AUTH_REQUEST_TOKEN_EMPTY("AUTH-REQUEST-1010", "访问令牌为空", 401),
    AUTH_REQUEST_TOKEN_EXPIRED("AUTH-REQUEST-1011", "访问令牌已过期", 419),

    AUTH_ACCOUNT_DISABLED("AUTH-ACCOUNT-1001", "账户状态异常", 403),
    AUTH_ACCOUNT_LOCKED("AUTH-ACCOUNT-1002", "账户状态异常（短时间登录失败次数过多被锁定）", 429),

    VAL_REQUEST_BODY_INVALID("VAL-REQUEST-1001", "请求体解析失败", 400),

    VAL_REQUEST_OSS_NO_FILE("VAL-REQUEST-OSS-1001", "未检测到上传文件", 400),
    VAL_REQUEST_OSS_FILE_EMPTY("VAL-REQUEST-OSS-1002", "文件内容为空", 422),

    VAL_PARAM_AUTH_LOGIN_TYPE_EMPTY("VAL-PARAM-AUTH-1001", "loginType字段为空", 422),
    VAL_RANGE_AUTH_LOGIN_TYPE_ERROR("VAL-RANGE-AUTH-1001", "loginType字段范围错误", 422),
    VAL_PARAM_AUTH_USERNAME_EMPTY("VAL-PARAM-AUTH-1002", "username字段为空", 422),
    VAL_PARAM_AUTH_EMAIL_EMPTY("VAL-PARAM-AUTH-1003", "email字段为空", 422),
    VAL_PARAM_AUTH_PASSWORD_EMPTY("VAL-PARAM-AUTH-1004", "password字段为空", 422),
    VAL_FORMAT_AUTH_EMAIL_ERROR("VAL-FORMAT-AUTH-1001", "email格式错误", 422),
    VAL_PARAM_AUTH_CARD_UUID_EMPTY("VAL-PARAM-AUTH-1005", "cardUuid字段为空", 422),
    VAL_PARAM_AUTH_VERIFICATION_ID_EMPTY("VAL-PARAM-AUTH-1006", "verificationId字段为空", 422),
    VAL_PARAM_AUTH_PIN_EMPTY("VAL-PARAM-AUTH-1007", "pin字段为空", 422),
    VAL_PARAM_AUTH_ACCESS_TOKEN_EMPTY("VAL-PARAM-AUTH-1008", "accessToken字段为空", 422),
    VAL_PARAM_AUTH_CAPTCHA_ID_EMPTY("VAL-PARAM-AUTH-1009", "captchaId字段为空", 422),
    VAL_PARAM_AUTH_CAPTCHA_CODE_EMPTY("VAL-PARAM-AUTH-1010", "captchaCode字段为空", 422),
    VAL_FORMAT_AUTH_CARD_UUID_ERROR("VAL-FORMAT-AUTH-1002", "cardUuid格式错误", 422),

    VAL_PARAM_USER_ID_EMPTY("VAL-PARAM-USER-1001", "userId字段为空", 422),
    VAL_PARAM_USER_PASSWORD_EMPTY("VAL-PARAM-USER-1002", "password字段为空", 422),

    VAL_RANGE_OSS_EXPIRES_OUT_OF_RANGE("VAL-RANGE-OSS-1001", "MinIO临时访问链接有效期超出范围", 422),

    SYS_IO_OSS_SERVICE_UNAVAILABLE("SYS-IO-OSS-1001", "文件存储服务不可用或连接异常", 503),
    SYS_IO_OSS_PRESIGN_ERROR("SYS-IO-OSS-1002", "获取MinIO临时访问链接异常", 500),
    SYS_IO_OSS_STAT_ERROR("SYS-IO-OSS-1003", "获取MinIO文件状态异常", 500),
    SYS_IO_OSS_UPLOAD_ERROR("SYS-IO-OSS-1004", "上传文件到MinIO失败", 500),
    SYS_IO_OSS_DELETE_ERROR("SYS-IO-OSS-1005", "删除MinIO文件失败", 500);

    private final String code;

    private final String message;

    private final int httpStatus;

    ErrorCode(String code, String message, int httpStatus) {
        this.code = code;
        this.message = message;
        this.httpStatus = httpStatus;
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public int getHttpStatus() {
        return httpStatus;
    }
}
