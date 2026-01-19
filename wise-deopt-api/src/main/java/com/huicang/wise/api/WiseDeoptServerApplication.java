package com.huicang.wise.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * 类功能描述：应用启动入口
 *
 * @author xingchentye
 * @date 2026-01-19
 * @modified xingchentye 2026-01-19 创建Spring Boot启动类
 */
@SpringBootApplication(scanBasePackages = "com.huicang.wise")
@EntityScan(basePackages = "com.huicang.wise.infrastructure.repository")
@EnableJpaRepositories(basePackages = "com.huicang.wise.infrastructure.repository")
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
