package com.huicang.wise.infrastructure.redis.aspect;

import com.huicang.wise.infrastructure.redis.RedisCacheUtils;
import com.huicang.wise.infrastructure.redis.annotation.CacheEvict;
import com.huicang.wise.infrastructure.redis.annotation.CacheEvicts;
import com.huicang.wise.infrastructure.redis.annotation.Cacheable;
import java.lang.reflect.Method;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;

/**
 * Redis缓存切面 处理@Cacheable和@CacheEvict注解，实现方法的缓存功能
 *
 * @author WiseDepot
 * @version 0.0.27
 * @since 2026-03-21
 */
@Aspect
@Component
public class RedisCacheAspect {

    private static final Logger logger = LoggerFactory.getLogger(RedisCacheAspect.class);

    private final ExpressionParser parser = new SpelExpressionParser();
    private final ParameterNameDiscoverer nameDiscoverer = new DefaultParameterNameDiscoverer();

    /**
     * 处理@Cacheable注解
     *
     * @param joinPoint 连接点
     * @param cacheable 缓存注解
     * @return 方法执行结果
     * @throws Throwable 异常
     */
    @Around("@annotation(cacheable)")
    public Object handleCacheable(ProceedingJoinPoint joinPoint, Cacheable cacheable)
            throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        Object[] args = joinPoint.getArgs();

        String cacheKey = generateCacheKey(cacheable.prefix(), cacheable.key(), method, args);

        if (cacheable.hash()) {
            String hashField = parseExpression(cacheKey, method, args);
            Object cachedValue = RedisCacheUtils.hGet(cacheKey, hashField);
            if (cachedValue != null) {
                logger.debug("从哈希缓存中获取数据. key: {}, field: {}", cacheKey, hashField);
                return cachedValue;
            }
        } else {
            Object cachedValue = RedisCacheUtils.get(cacheKey);
            if (cachedValue != null) {
                logger.debug("从缓存中获取数据，key: {}", cacheKey);
                return cachedValue;
            }
        }

        logger.debug("缓存未命中，执行方法，key: {}", cacheKey);
        Object result = joinPoint.proceed();

        if (result != null
                && evaluateCondition(
                        cacheable.condition(), cacheable.unless(), method, args, result)) {

            if (cacheable.hash()) {
                String hashField = parseExpression(cacheable.field(), method, args);
                Object cachedValue = RedisCacheUtils.hGet(cacheKey, hashField);
                if (cachedValue != null) {
                    logger.debug("从哈希缓存中获取数据, key: {}, field: {}", cacheKey, hashField);
                    return cachedValue;
                }
            } else {
                RedisCacheUtils.set(cacheKey, result, cacheable.timeout());
                logger.debug("将结果存入缓存，key: {}, timeout: {}s", cacheKey, cacheable.timeout());
            }
        }

        return result;
    }

    /**
     * 处理**重复**的 `@CacheEvict`（一个方法要作废多类缓存时）。
     *
     * <p>为什么不支持它就罢了：典型场景是"删除角色"——既失效 `role:<id>`，也失效 `auth:permission:*`。若只能写一个注解，第二个就只能退化成方法体里手动调
     * `RedisCacheUtils`，而那种静态服务定位器**在没有容器的单元测试里是 NPE**。 声明式地把两件事都写在注解上，测试就不必为了跑一个方法而搭半个 Spring。
     */
    @Around("@annotation(cacheEvicts)")
    public Object handleCacheEvicts(ProceedingJoinPoint joinPoint, CacheEvicts cacheEvicts)
            throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        Object[] args = joinPoint.getArgs();

        // 与方法体一致的顺序：先执行标了 beforeInvocation 的，方法体之后再执行其余的
        for (CacheEvict cacheEvict : cacheEvicts.value()) {
            if (cacheEvict.beforeInvocation()) {
                evictCache(cacheEvict, method, args);
            }
        }
        Object result = joinPoint.proceed();
        for (CacheEvict cacheEvict : cacheEvicts.value()) {
            if (!cacheEvict.beforeInvocation()) {
                evictCache(cacheEvict, method, args);
            }
        }
        return result;
    }

    /**
     * 处理@CacheEvict注解
     *
     * @param joinPoint 连接点
     * @param cacheEvict 缓存清除注解
     * @return 方法执行结果
     * @throws Throwable 异常
     */
    @Around("@annotation(cacheEvict)")
    public Object handleCacheEvict(ProceedingJoinPoint joinPoint, CacheEvict cacheEvict)
            throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        Object[] args = joinPoint.getArgs();

        if (cacheEvict.beforeInvocation()) {
            evictCache(cacheEvict, method, args);
        }

        Object result = joinPoint.proceed();

        if (!cacheEvict.beforeInvocation()) {
            evictCache(cacheEvict, method, args);
        }

        return result;
    }

    /**
     * 清除缓存
     *
     * @param cacheEvict 缓存清除注解
     * @param method 方法
     * @param args 方法参数
     */
    private void evictCache(CacheEvict cacheEvict, Method method, Object[] args) {
        if (!evaluateCondition(cacheEvict.condition(), "", method, args, null)) {
            return;
        }

        if (cacheEvict.allEntries()) {
            /*
             * `allEntries` 只能清**这个前缀**，绝不能清整个库。
             *
             * 2026-10-05 实测：这里原来是 `RedisCacheUtils.flushAll()`（`KEYS *` + `DEL`），
             * 而 Redis 里同时住着**安全状态** —— 登录失败计数 `auth:login:fail:*`、
             * 限流计数 `api:rate-limit:*`、人机验证的挑战/票据 `human:*`、请求 nonce `api:nonce:*`。
             * 于是"新建/修改一个仓库"（`WarehouseApplicationService` 上唯一的 `allEntries=true`）
             * 会把**暴力破解保护、限流、正在进行的验证**一起清掉 —— 一次正常的业务写，
             * 静默地把安全边界重置了。这属于"缓存清理把别人的状态当垃圾扫了"。
             *
             * 没有前缀就更不许清：那等于"我不知道该清什么，所以全清"。
             */
            if (cacheEvict.prefix() == null || cacheEvict.prefix().isEmpty()) {
                throw new IllegalStateException(
                        "@CacheEvict(allEntries = true) 必须带 prefix："
                                + "没有前缀就只能清空整个 Redis，那会把登录锁定/限流/人机验证的状态一起扫掉。"
                                + "请写明要清哪一类缓存（例如 prefix = \"warehouse\"）。");
            }
            String pattern = cacheEvict.prefix() + ":*";
            RedisCacheUtils.deleteByPattern(pattern);
            logger.info("按前缀清除缓存，pattern: {}", pattern);
        } else {
            String cacheKey = generateCacheKey(cacheEvict.prefix(), cacheEvict.key(), method, args);
            if (cacheEvict.hash()) {
                String hashField = parseExpression(cacheEvict.field(), method, args);
                RedisCacheUtils.hDelete(cacheKey, hashField);
                logger.debug("清除哈希缓存，key: {}, field: {}", cacheKey, hashField);
            } else {
                RedisCacheUtils.delete(cacheKey);
                logger.debug("清除缓存，key: {}", cacheKey);
            }
        }
    }

    /**
     * 生成缓存键
     *
     * @param prefix 前缀
     * @param key 键表达式
     * @param method 方法
     * @param args 方法参数
     * @return 缓存键
     */
    private String generateCacheKey(String prefix, String key, Method method, Object[] args) {
        String keyPart = parseExpression(key, method, args);
        if (prefix.isEmpty()) {
            return keyPart;
        }
        return prefix + ":" + keyPart;
    }

    /**
     * 解析SpEL表达式
     *
     * @param expression 表达式
     * @param method 方法
     * @param args 方法参数
     * @return 解析结果
     */
    private String parseExpression(String expression, Method method, Object[] args) {
        if (expression == null || expression.isEmpty()) {
            return "";
        }

        EvaluationContext context = createEvaluationContext(method, args);
        Expression exp = parser.parseExpression(expression);
        Object value = exp.getValue(context);
        return value != null ? value.toString() : "";
    }

    /**
     * 创建SpEL表达式上下文
     *
     * @param method 方法
     * @param args 方法参数
     * @return 表达式上下文
     */
    private EvaluationContext createEvaluationContext(Method method, Object[] args) {
        StandardEvaluationContext context = new StandardEvaluationContext();
        String[] parameterNames = nameDiscoverer.getParameterNames(method);
        if (parameterNames != null) {
            for (int i = 0; i < parameterNames.length; i++) {
                context.setVariable(parameterNames[i], args[i]);
            }
        }
        return context;
    }

    /**
     * 评估条件表达式
     *
     * @param condition 条件表达式
     * @param unless 排除条件表达式
     * @param method 方法
     * @param args 方法参数
     * @param result 方法执行结果
     * @return 是否满足条件
     */
    private boolean evaluateCondition(
            String condition, String unless, Method method, Object[] args, Object result) {
        EvaluationContext context = createEvaluationContext(method, args);
        if (result != null) {
            context.setVariable("result", result);
        }

        if (condition != null && !condition.isEmpty()) {
            Expression exp = parser.parseExpression(condition);
            Boolean conditionResult = exp.getValue(context, Boolean.class);
            if (conditionResult == null || !conditionResult) {
                return false;
            }
        }

        if (unless != null && !unless.isEmpty()) {
            Expression exp = parser.parseExpression(unless);
            Boolean unlessResult = exp.getValue(context, Boolean.class);
            if (unlessResult != null && unlessResult) {
                return false;
            }
        }

        return true;
    }
}
