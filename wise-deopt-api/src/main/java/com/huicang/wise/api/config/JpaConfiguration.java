package com.huicang.wise.api.config;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * JPA配置类
 * 配置实体扫描路径和仓储扫描路径
 *
 * @author WiseDepot
 * @version 0.0.21
 * @since 2026-01-22
 */
@Configuration
@EntityScan(basePackages = {"com.huicang.wise.infrastructure.repository", "com.huicang.wise.domain"})
@EnableJpaRepositories(basePackages = {"com.huicang.wise.infrastructure.repository", "com.huicang.wise.domain.request", "com.huicang.wise.domain.repository"})
public class JpaConfiguration {
}
