package com.huicang.wise.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 类功能描述：应用启动入口
 *
 * <p>**不再声明 {@code @EntityScan}**（2026-02-27）。原先这里与 {@code
 * com.huicang.wise.infrastructure.config.JpaConfiguration} 各有一份 {@code @EntityScan}，
 * 两处声明的作用范围不同（本处还包含已废弃的 {@code infrastructure.repository} 与根本不存在的 {@code domain.request}），而 Spring
 * Boot 的 {@code EntityScanPackages} 对多次注册的合并语义并不直观—— 结果就是"实际扫描到哪些包"依赖两者谁先被解析，属于隐式行为。
 *
 * <p>持久化配置已按 STD-ARCH-02 归位到 infrastructure 的 {@code JpaConfiguration}，实体扫描范围以那里为
 * **唯一来源**；入口层不再持有持久化扫描知识。
 *
 * @author xingchentye
 * @date 2026-01-19
 * @modified xingchentye 2026-01-19 创建Spring Boot启动类
 * @modified WiseDepot 2026-02-27 移除重复且过期的 @EntityScan，扫描范围统一由 JpaConfiguration 声明
 */
@SpringBootApplication(scanBasePackages = "com.huicang.wise")
@EnableScheduling
@EnableAsync
public class WiseDeoptServerApplication {

    /**
     * 方法功能描述：应用主入口
     *
     * @param args 启动参数
     * @return 无
     */
    public static void main(String[] args) {
        SpringApplication.run(WiseDeoptServerApplication.class, args);
    }
}
