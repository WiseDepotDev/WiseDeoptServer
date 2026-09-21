package com.huicang.wise.api.aspect;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.multipart.MultipartFile;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 类功能描述：Web请求日志切面
 *
 * <p>P5-06（M-17 派生）：请求/响应日志必须**脱敏**后再打印。此前这里直接输出
 * {@code objectMapper.writeValueAsString(args)}，登录、注册、改密等接口的明文口令、
 * 令牌会整段写进日志文件，属于"全量请求体日志"这一类的安全事故面。
 *
 * <p>脱敏规则（见 {@link #maskSensitive(String)}）：
 * <ol>
 *   <li>按**字段名**命中：password / passwd / pwd / token / refreshToken / secret /
 *       signature / authorization / apiKey / captcha / pin 等；</li>
 *   <li>按**值形态**兜底：任何以 {@code eyJ} 开头的 JWT（即使字段名不认识）也掩码。</li>
 * </ol>
 * 掩码只影响日志文本，不改变返回给客户端的数据。
 *
 * @author xingchentye
 * @date 2026-01-23
 */
@Aspect
@Component
public class WebLogAspect {

    private static final Logger log = LoggerFactory.getLogger(WebLogAspect.class);

    /**
     * 命中敏感字段名时，把其字符串值替换为 ***（保持 JSON 结构，便于日志比对）。
     *
     * <p>两类写法：无歧义的词根允许**前后缀**（覆盖 {@code X-Signature}、{@code signatureSecret}、
     * {@code newPassword} 这类命名）；有歧义的短词只做**全词**匹配（{@code pin} 不该命中
     * {@code shipping}）。只匹配字符串值，因此 {@code tokenCount: 3} 这类数字字段不受影响。
     *
     * <p>新增敏感字段名时，请同时补 {@code WebLogAspectMaskTest} 用例。
     */
    private static final Pattern SENSITIVE_FIELD_PATTERN = Pattern.compile(
            "\"((?:[A-Za-z0-9_-]*(?:password|passwd|token|secret|signature|apikey|privatekey"
                    + "|authorization|captcha)[A-Za-z0-9_-]*)|(?:pwd|pin|nfcuid))\"\\s*:\\s*\"[^\"]*\"",
            Pattern.CASE_INSENSITIVE);

    /** 值形态兜底：JWT（header.payload.signature）无论挂在哪个字段名下都掩码。 */
    private static final Pattern JWT_VALUE_PATTERN = Pattern.compile(
            "\"eyJ[A-Za-z0-9_-]{5,}\\.[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]*\"");

    /**
     * 对即将写入日志的 JSON 文本做脱敏（纯函数，便于单测）。
     *
     * @param json 序列化后的请求参数/响应体，可为 null
     * @return 脱敏后的文本；入参为 null 时返回 null
     */
    public static String maskSensitive(String json) {
        if (json == null || json.isEmpty()) {
            return json;
        }
        String masked = SENSITIVE_FIELD_PATTERN.matcher(json)
                .replaceAll(match -> "\"" + match.group(1) + "\":\"***\"");
        masked = JWT_VALUE_PATTERN.matcher(masked).replaceAll("\"***\"");
        return masked;
    }

    /**
     * 使用 Spring 管理的 ObjectMapper（包含 JavaTimeModule 等配置），避免日志序列化时频繁出现
     * “Unable to serialize result” 的误报。
     */
    @Autowired
    private ObjectMapper objectMapper;

    /**
     * 定义切入点，拦截 controller 包下的所有方法
     */
    @Pointcut("execution(public * com.huicang.wise.api.controller..*.*(..))")
    public void webLog() {
    }

    /**
     * 环绕通知
     *
     * @param joinPoint 切入点
     * @return 方法执行结果
     * @throws Throwable 异常
     */
    @Around("webLog()")
    public Object doAround(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();
        
        // 获取当前请求对象
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = attributes != null ? attributes.getRequest() : null;

        String url = request != null ? request.getRequestURL().toString() : "N/A";
        String method = request != null ? request.getMethod() : "N/A";
        String ip = request != null ? request.getRemoteAddr() : "N/A";
        String className = joinPoint.getSignature().getDeclaringTypeName();
        String methodName = joinPoint.getSignature().getName();

        // 排除心跳接口日志
        if ("receiveHeartbeat".equals(methodName) && "com.huicang.wise.api.controller.DeviceController".equals(className)) {
            return joinPoint.proceed();
        }

        // 打印请求日志
        log.info("========================================== Start ==========================================");
        log.info("URL          : {}", url);
        log.info("Method       : {}", method);
        log.info("IP           : {}", ip);
        log.info("Class Method : {}.{}", className, methodName);
        
        try {
            // 尝试打印请求参数，忽略过长或无法序列化的参数
            Object[] args = joinPoint.getArgs();
            if (args != null && args.length > 0) {
                 try {
                     List<Object> logArgs = Arrays.stream(args)
                         .map(arg -> {
                             if (arg instanceof MultipartFile) {
                                 MultipartFile file = (MultipartFile) arg;
                                 return "File: " + file.getOriginalFilename() + " (" + file.getSize() + " bytes)";
                             } else if (arg instanceof HttpServletRequest) {
                                 return "HttpServletRequest";
                             } else if (arg instanceof HttpServletResponse) {
                                 return "HttpServletResponse";
                             } else if (arg instanceof byte[]) {
                                 return "byte[" + ((byte[]) arg).length + "]";
                             }
                             return arg;
                         })
                         .collect(Collectors.toList());
                     
                     log.info("Request Args : {}", maskSensitive(objectMapper.writeValueAsString(logArgs)));
                 } catch (Exception e) {
                     log.warn("Request Args : Unable to serialize args");
                 }
            }
        } catch (Exception e) {
            // 忽略参数打印异常，不影响主流程
        }

        Object result;
        try {
            result = joinPoint.proceed();
        } catch (Throwable e) {
            long timeCost = System.currentTimeMillis() - startTime;
            log.error("Exception    : {} ({}ms)", e.getMessage(), timeCost);
            log.info("========================================== Error ==========================================");
            throw e;
        }

        long timeCost = System.currentTimeMillis() - startTime;
        
        // 打印响应日志
        try {
            if (result == null) {
                log.info("Response     : null (void or empty)");
            } else if (result instanceof byte[]) {
                log.info("Response     : byte[{}]", ((byte[]) result).length);
            } else if (result instanceof MultipartFile) {
                log.info("Response     : MultipartFile");
            } else {
                // 登录/注册等接口的响应体里带 token / refreshToken，同样必须脱敏（P5-06）
                log.info("Response     : {}", maskSensitive(objectMapper.writeValueAsString(result)));
            }
        } catch (Exception e) {
            log.warn("Response     : Unable to serialize result");
        }
        
        log.info("Time Cost    : {}ms", timeCost);
        log.info("=========================================== End ===========================================");
        
        return result;
    }
}
