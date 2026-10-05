package com.huicang.wise.infrastructure.redis;

/**
 * Redis 键名的**唯一存放处**。
 *
 * <p>## 为什么需要一个类
 *
 * <p>键名是**契约**：写的一方与清的一方必须字面一致，而两者常常不在同一个类里 （看板 KPI 被 5 个服务失效过）。这类"散落的字符串"出问题的样子特别隐蔽 ——
 * 改了其中一处，另一处仍然"成功执行"，只是**清了个不存在的键**：没有任何异常、 没有任何日志，表现为"看板数字陈旧 1 分钟"这种最难查的 bug。
 *
 * <p>所以规矩是：**生产代码里不许出现 Redis 键的字面量**（本文件除外）。 `RedisKeyLiteralTest` 会把这件事变成机械约束（新增一处散落字面量就红）。
 *
 * <p>注解里的 `prefix` 也读这里的常量 —— 注解值要求编译期常量，而 `public static final String`
 * 正好满足，于是"注解拼出来的键"与"手写的键"共用同一个定义。
 */
public final class RedisKeys {

    private RedisKeys() {}

    // ---------------------------------------------------------------- 看板

    /**
     * 看板 KPI 汇总（JSON 字符串），TTL 1 分钟。
     *
     * <p>写入方：{@code DashboardApplicationService}（读缓存未命中时才重算）。 失效方：**任何改动这些数字的业务写**（告警、设备、巡检、库存……），
     * 它们只知道"数字变了"，不知道 KPI 是怎么算的 —— 所以这里是一个跨类的公共键。
     */
    public static final String DASHBOARD_KPI = "dashboard:kpi";

    // ---------------------------------------------------------------- 库存

    /** 库存缓存的前缀（`@Cacheable(prefix = …)` 与下面的手工键共用）。 */
    public static final String INVENTORY = "inventory";

    /** 库存总量（跨商品聚合），TTL 300s。由 {@link #INVENTORY} 派生，避免两处拼写。 */
    public static final String INVENTORY_TOTAL = INVENTORY + ":total";

    /** 单商品库存汇总，TTL 30 分钟（由 `cacheInventorySummary` 手工写入）。 */
    public static String inventorySummary(Object productId) {
        return INVENTORY + ":summary:" + productId;
    }

    // ---------------------------------------------------------------- 告警

    /**
     * "未处理告警"的最近若干条（列表，TTL 6 小时），供看板/设备页快速展示。
     *
     * <p>它是**视图缓存**而不是事实：真正的告警在 `alert_event` 表里，这个列表丢了只是要重新查库。
     */
    public static final String ALERT_UNHANDLED_LIST = "alert:unhandled:list";

    // ---------------------------------------------------------------- 消息

    /** 消息实体缓存的前缀（`@Cacheable(prefix = …)` 用）。 */
    public static final String MESSAGE = "message";

    /**
     * 未读数缓存的前缀，TTL 60s（键形如 `message:unread:<receiverId>`）。
     *
     * <p>为什么它值得缓存：桥**每 10 秒**为每个在线客户端轮询一次未读数，而它是一条 `COUNT(*) … WHERE receiver_id = ? AND is_read =
     * 0`。单个客户端一天就是 8640 次查询， 多客户端线性增长 —— 这是全服务端最热的一次读。
     *
     * <p>正确性靠**写侧失效**（新增消息 / 标记已读 / 删除都会清），TTL 只是兜底： 万一哪条写路径漏了失效，最坏也只是"未读数最多迟 60 秒"，不会永久错。
     */
    public static final String MESSAGE_UNREAD = MESSAGE + ":unread";

    /** 某个接收者的未读缓存键。 */
    public static String messageUnread(Long receiverId) {
        return MESSAGE_UNREAD + ":" + receiverId;
    }

    // ---------------------------------------------------------------- 巡检

    /**
     * 巡检任务实体缓存的前缀，TTL 1800s。
     *
     * <p>它在两处出现：方法上的 `@CacheEvict(prefix = …)`／`@Cacheable(prefix = …)`，
     * 以及"结果上报"那条流程里的手工清除（那个方法的参数是请求体，注解表达式取不到 taskId）。 两边都由这个常量派生 —— 否则改了注解、手工那行会**静默失效**。
     */
    public static final String INSPECTION_TASK = "inspection:task";

    public static String inspectionTask(Object taskId) {
        return INSPECTION_TASK + ":" + taskId;
    }
}
