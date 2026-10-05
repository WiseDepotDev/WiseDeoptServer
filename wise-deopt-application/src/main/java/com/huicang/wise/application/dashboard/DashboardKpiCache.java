package com.huicang.wise.application.dashboard;

import com.huicang.wise.infrastructure.redis.RedisCacheUtils;
import com.huicang.wise.infrastructure.redis.RedisKeys;
import java.time.Duration;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 看板 KPI 缓存（`dashboard:kpi`）的**唯一 owner**。
 *
 * <p>## 为什么要有一个组件，而不是各处直接写键
 *
 * <p>这个数字被 **5 个服务**在业务写之后失效过（告警 2、设备 6、巡检 1、库存 1）。 它们只知道"某个数字变了"，不知道 KPI 是怎么算的 ——
 * 这正是"键名与失效责任散落多处"的典型。 之前那种写法有两个后果：
 *
 * <ol>
 *   <li><b>键名改一处漏一处</b>：漏掉的那处会"成功执行"，只是删了一个不存在的键（无异常、无日志）；
 *   <li><b>失败语义不统一</b>：直接调 `stringRedisTemplate` 时 Redis 一抖就把**业务写**一起带崩（500）， 而缓存失效失败本该只是"看板晚 1
 *       分钟更新"。
 * </ol>
 *
 * <p>所以失效入口只有 `invalidate()`：键名在这里、失败语义也在这里（经 {@link RedisCacheUtils}， 内部
 * catch+log，**绝不让缓存问题变成业务失败**）。测试可以 mock 它并断言 `invalidate()` 被调用 —— 静态工具类做不到这一点。
 */
@Component
public class DashboardKpiCache {

    private static final Logger log = LoggerFactory.getLogger(DashboardKpiCache.class);

    /** KPI 的存活时间：它不是实时数，1 分钟足够；失效路径才是正确性的保证。 */
    private static final Duration TTL = Duration.ofMinutes(1);

    /** 取缓存的 KPI（JSON 字符串）。空闲时返回空 —— 调用方据此重算。 */
    public Optional<String> get() {
        return Optional.ofNullable(RedisCacheUtils.get(RedisKeys.DASHBOARD_KPI, String.class));
    }

    /** 写入 KPI（由看板服务在重算后调用）。 */
    public void put(String kpiJson) {
        RedisCacheUtils.set(RedisKeys.DASHBOARD_KPI, kpiJson, TTL.getSeconds());
    }

    /**
     * 失效（任何改动了看板数字的业务写都调它）。
     *
     * <p>失败只记日志：这是**缓存**失效，不是业务事实 —— 让它把业务写带崩属于失败方向搞反。
     */
    public void invalidate() {
        try {
            RedisCacheUtils.delete(RedisKeys.DASHBOARD_KPI);
        } catch (RuntimeException e) {
            // RedisCacheUtils 自己已经 catch 过一层；这里再兜一层是防止将来有人换掉底层实现
            log.warn("看板 KPI 缓存失效失败（不影响业务写）: {}", e.getMessage());
        }
    }
}
