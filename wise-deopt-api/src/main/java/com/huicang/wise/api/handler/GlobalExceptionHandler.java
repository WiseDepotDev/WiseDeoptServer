package com.huicang.wise.api.handler;

import com.huicang.wise.common.log.ErrorLogStorageService;
import com.huicang.wise.common.log.ServerErrorLogDTO;
import com.huicang.wise.common.api.ApiResponse;
import com.huicang.wise.common.api.ErrorCode;
import com.huicang.wise.common.exception.AuthenticationException;
import com.huicang.wise.common.exception.AuthorizationException;
import com.huicang.wise.common.exception.BusinessException;
import com.huicang.wise.common.exception.NotFoundException;
import com.huicang.wise.common.exception.ValidationException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.ContentCachingRequestWrapper;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 全局异常处理器
 *
 * @author WiseDepot
 * @version 0.0.28
 * @since 2026-02-27
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @Autowired(required = false)
    private ErrorLogStorageService errorLogStorageService;

    /**
     * 处理业务异常
     *
     * @param ex 业务异常对象
     * @return 统一响应结果
     */
    @ExceptionHandler(BusinessException.class)
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<Void> handleBusinessException(BusinessException ex, HttpServletRequest request) {
        log.warn("业务异常: code={}, message={}, uri={}", 
                ex.getErrorCode().getCode(), ex.getMessage(), request.getRequestURI());
        return ApiResponse.failure(ex.getErrorCode(), ex.getMessage());
    }

    /**
     * 处理资源未找到异常
     *
     * @param ex 资源未找到异常对象
     * @return 统一响应结果
     */
    @ExceptionHandler(NotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiResponse<Void> handleNotFoundException(NotFoundException ex, HttpServletRequest request) {
        log.warn("资源未找到: code={}, message={}, uri={}", 
                ex.getErrorCode().getCode(), ex.getMessage(), request.getRequestURI());
        return ApiResponse.failure(ex.getErrorCode(), ex.getMessage());
    }

    /**
     * 处理认证异常
     *
     * @param ex 认证异常对象
     * @return 统一响应结果
     */
    @ExceptionHandler(AuthenticationException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ApiResponse<Void> handleAuthenticationException(AuthenticationException ex, HttpServletRequest request) {
        log.warn("认证异常: code={}, message={}, uri={}", 
                ex.getErrorCode().getCode(), ex.getMessage(), request.getRequestURI());
        return ApiResponse.failure(ex.getErrorCode(), ex.getMessage());
    }

    /**
     * 处理授权异常
     *
     * @param ex 授权异常对象
     * @return 统一响应结果
     */
    @ExceptionHandler(AuthorizationException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ApiResponse<Void> handleAuthorizationException(AuthorizationException ex, HttpServletRequest request) {
        log.warn("授权异常: code={}, message={}, uri={}", 
                ex.getErrorCode().getCode(), ex.getMessage(), request.getRequestURI());
        return ApiResponse.failure(ex.getErrorCode(), ex.getMessage());
    }

    /**
     * 处理参数校验异常
     *
     * @param ex 参数校验异常对象
     * @return 统一响应结果
     */
    @ExceptionHandler(ValidationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleValidationException(ValidationException ex, HttpServletRequest request) {
        log.warn("参数校验异常: code={}, message={}, uri={}", 
                ex.getErrorCode().getCode(), ex.getMessage(), request.getRequestURI());
        return ApiResponse.failure(ex.getErrorCode(), ex.getMessage());
    }

    /**
     * 处理方法参数校验异常
     *
     * @param ex 参数校验异常对象
     * @return 统一响应结果
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleMethodArgumentNotValidException(MethodArgumentNotValidException ex, HttpServletRequest request) {
        String errorMessage = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining(", "));
        log.warn("方法参数校验异常: {}, uri={}", errorMessage, request.getRequestURI());
        return ApiResponse.failure(ErrorCode.PARAM_ERROR, "请求参数校验失败: " + errorMessage);
    }

    /**
     * 处理约束违反异常
     *
     * @param ex 约束违反异常对象
     * @return 统一响应结果
     */
    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleConstraintViolationException(ConstraintViolationException ex, HttpServletRequest request) {
        String errorMessage = ex.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.joining(", "));
        log.warn("约束违反异常: {}, uri={}", errorMessage, request.getRequestURI());
        return ApiResponse.failure(ErrorCode.PARAM_ERROR, "请求参数校验失败: " + errorMessage);
    }

    /**
     * 处理请求体解析失败异常
     *
     * @param ex 请求体解析失败异常对象
     * @return 统一响应结果
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleHttpMessageNotReadableException(HttpMessageNotReadableException ex, HttpServletRequest request) {
        log.warn("请求体解析失败: {}, uri={}", ex.getMessage(), request.getRequestURI());
        return ApiResponse.failure(ErrorCode.VAL_REQUEST_BODY_INVALID, ErrorCode.VAL_REQUEST_BODY_INVALID.getMessage());
    }

    /**
     * 处理不支持的请求方法异常
     *
     * @param ex 不支持的请求方法异常对象
     * @return 统一响应结果
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    @ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
    public ApiResponse<Void> handleHttpRequestMethodNotSupportedException(HttpRequestMethodNotSupportedException ex, HttpServletRequest request) {
        log.warn("不支持的请求方法: {}, uri={}", ex.getMessage(), request.getRequestURI());
        return ApiResponse.failure(ErrorCode.REQUEST_METHOD_NOT_ALLOWED, ErrorCode.REQUEST_METHOD_NOT_ALLOWED.getMessage());
    }

    /**
     * 处理缺少必需的请求参数异常
     *
     * @param ex 缺少必需的请求参数异常对象
     * @return 统一响应结果
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleMissingServletRequestParameterException(MissingServletRequestParameterException ex, HttpServletRequest request) {
        log.warn("缺少必需的请求参数: {}, uri={}", ex.getParameterName(), request.getRequestURI());
        return ApiResponse.failure(ErrorCode.PARAM_ERROR, "缺少必需的请求参数: " + ex.getParameterName());
    }

    /**
     * 处理请求参数类型不匹配异常（如路径变量/查询参数无法转换为期望类型）。
     *
     * <p>此类请求属于客户端参数错误，必须返回 400 而非 500；
     * 此前缺少本处理器时会落入兜底分支返回 SYS 类 500，造成「非法输入被当作系统故障」的误导。
     *
     * @param ex 类型不匹配异常对象
     * @param request 当前请求
     * @return 统一响应结果
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleMethodArgumentTypeMismatchException(MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        log.warn("请求参数类型不匹配: name={}, value={}, uri={}",
                ex.getName(), ex.getValue(), request.getRequestURI());
        return ApiResponse.failure(ErrorCode.PARAM_ERROR,
                "请求参数类型不合法: " + ex.getName());
    }

    /**
     * 处理响应状态异常
     *
     * @param ex 响应状态异常对象
     * @return 统一响应结果
     */
    @ExceptionHandler(ResponseStatusException.class)
    public ApiResponse<Void> handleResponseStatusException(ResponseStatusException ex, HttpServletRequest request) {
        log.warn("HTTP响应状态异常: status={}, message={}, uri={}", 
                ex.getStatusCode(), ex.getMessage(), request.getRequestURI());
        if (HttpStatus.NOT_FOUND.equals(ex.getStatusCode())) {
            return ApiResponse.failure(ErrorCode.REQUEST_NOT_FOUND, ErrorCode.REQUEST_NOT_FOUND.getMessage());
        }
        if (HttpStatus.METHOD_NOT_ALLOWED.equals(ex.getStatusCode())) {
            return ApiResponse.failure(ErrorCode.REQUEST_METHOD_NOT_ALLOWED, ErrorCode.REQUEST_METHOD_NOT_ALLOWED.getMessage());
        }
        if (HttpStatus.UNAUTHORIZED.equals(ex.getStatusCode())) {
            return ApiResponse.failure(ErrorCode.UNAUTHORIZED, ErrorCode.UNAUTHORIZED.getMessage());
        }
        if (HttpStatus.FORBIDDEN.equals(ex.getStatusCode())) {
            return ApiResponse.failure(ErrorCode.FORBIDDEN, ErrorCode.FORBIDDEN.getMessage());
        }
        return ApiResponse.failure(ErrorCode.SYSTEM_REQUEST_ERROR, ErrorCode.SYSTEM_REQUEST_ERROR.getMessage());
    }

    /**
     * 处理静态资源未找到异常
     *
     * @param ex 资源未找到异常对象
     * @return 统一响应结果
     */
    @ExceptionHandler(NoResourceFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiResponse<Void> handleNoResourceFoundException(NoResourceFoundException ex, HttpServletRequest request) {
        log.warn("资源未找到: {}, uri={}", ex.getResourcePath(), request.getRequestURI());
        return ApiResponse.failure(ErrorCode.REQUEST_NOT_FOUND, "资源不存在: " + ex.getResourcePath());
    }

    /**
     * 处理处理器未找到异常
     *
     * @param ex 处理器未找到异常对象
     * @return 统一响应结果
     */
    @ExceptionHandler(NoHandlerFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiResponse<Void> handleNoHandlerFoundException(NoHandlerFoundException ex, HttpServletRequest request) {
        log.warn("处理器未找到: {}, uri={}", ex.getRequestURL(), request.getRequestURI());
        return ApiResponse.failure(ErrorCode.REQUEST_NOT_FOUND, "接口不存在: " + ex.getRequestURL());
    }

    /**
     * 处理系统未知异常
     *
     * @param ex 未知异常对象
     * @return 统一响应结果
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiResponse<Void> handleException(Exception ex, HttpServletRequest request) {
        log.error("系统未知异常: uri={}", request.getRequestURI(), ex);
        
        // 记录异常到MinIO
        if (errorLogStorageService != null) {
            try {
                ServerErrorLogDTO logDTO = buildErrorLog(ex, request);
                errorLogStorageService.storeErrorLog(logDTO);
            } catch (Exception e) {
                log.error("Failed to store error log", e);
            }
        }
        
        return ApiResponse.failure(ErrorCode.SYSTEM_ERROR, "系统异常，请联系管理员");
    }

    private ServerErrorLogDTO buildErrorLog(Exception ex, HttpServletRequest request) {
        // 提取堆栈信息
        StringWriter sw = new StringWriter();
        ex.printStackTrace(new PrintWriter(sw));
        String stackTrace = sw.toString();

        // 提取请求头
        Map<String, String> headers = new HashMap<>();
        Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            headers.put(headerName, request.getHeader(headerName));
        }

        // 提取请求体
        String requestBody = "";
        if (request instanceof ContentCachingRequestWrapper) {
            ContentCachingRequestWrapper wrapper = (ContentCachingRequestWrapper) request;
            byte[] content = wrapper.getContentAsByteArray();
            if (content.length > 0) {
                requestBody = new String(content, StandardCharsets.UTF_8);
            }
        }

        // 获取服务实例ID
        String instanceId = "unknown";
        try {
            instanceId = InetAddress.getLocalHost().getHostAddress();
        } catch (Exception e) {
            // ignore
        }

        return ServerErrorLogDTO.builder()
                .stackTrace(stackTrace)
                .requestUrl(request.getRequestURI())
                .requestMethod(request.getMethod())
                .requestHeaders(headers)
                .requestBody(requestBody)
                .timestamp(LocalDateTime.now())
                .serviceInstanceId(instanceId)
                .build();
    }
}
