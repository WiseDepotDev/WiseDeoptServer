package com.huicang.wise.api.config;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * 类功能描述：JPA配置类
 *
 * @author xingchentye
 * @date 2026-01-22
 */
@Configuration
@EntityScan(basePackages = "com.huicang.wise.infrastructure.repository")
@EnableJpaRepositories(basePackages = "com.huicang.wise.infrastructure.repository")
public class JpaConfiguration {
}
