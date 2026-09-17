package com.huicang.wise.infrastructure.datasource;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

/**
 * 多数据源配置类
 * 配置动态路由数据源和各模块独立数据源
 *
 * @author WiseDepot
 * @version 0.0.27
 * @since 2026-02-27
 */
@Configuration
@ConditionalOnProperty(name = "wise.datasource.multi.enabled", havingValue = "true", matchIfMissing = false)
@EnableConfigurationProperties(MultiDataSourceProperties.class)
public class MultiDataSourceConfiguration {

    private static final Logger log = LoggerFactory.getLogger(MultiDataSourceConfiguration.class);

    @Value("${spring.datasource.url:jdbc:mysql://10.0.0.4:3306/wise_depot}")
    private String defaultUrl;

    @Value("${spring.datasource.username:root}")
    private String defaultUsername;

    @Value("${spring.datasource.password:password}")
    private String defaultPassword;

    @Value("${spring.datasource.driver-class-name:com.mysql.cj.jdbc.Driver}")
    private String defaultDriverClassName;

    @Value("${wise.datasource.hikari.minimum-idle:2}")
    private int minimumIdle;

    @Value("${wise.datasource.hikari.maximum-pool-size:10}")
    private int maximumPoolSize;

    @Value("${wise.datasource.hikari.idle-timeout:300000}")
    private long idleTimeout;

    @Value("${wise.datasource.hikari.connection-timeout:20000}")
    private long connectionTimeout;

    @Value("${wise.datasource.hikari.max-lifetime:1200000}")
    private long maxLifetime;

    @Value("${wise.datasource.hikari.connection-test-query:SELECT 1}")
    private String connectionTestQuery;

    /**
     * 创建动态路由数据源
     * 作为主数据源，根据上下文路由到具体的数据库
     *
     * @param properties 多数据源配置属性
     * @return 动态路由数据源
     */
    @Bean
    @Primary
    public DataSource dynamicDataSource(MultiDataSourceProperties properties) {
        DynamicRoutingDataSource routingDataSource = new DynamicRoutingDataSource();

        Map<Object, Object> targetDataSources = new HashMap<>();

        for (DatabaseType dbType : DatabaseType.values()) {
            DataSource dataSource = createDataSourceForDatabase(dbType, properties);
            targetDataSources.put(dbType, dataSource);
            log.info("已配置数据源: {} ({})", dbType.getCode(), dbType.getDescription());
        }

        routingDataSource.setTargetDataSources(targetDataSources);

        DataSource defaultDataSource = (DataSource) targetDataSources.get(DatabaseType.USER);
        routingDataSource.setDefaultTargetDataSource(defaultDataSource);

        return routingDataSource;
    }

    /**
     * 为指定数据库类型创建数据源
     *
     * @param dbType    数据库类型
     * @param properties 多数据源配置属性
     * @return 数据源对象
     */
    private DataSource createDataSourceForDatabase(DatabaseType dbType, MultiDataSourceProperties properties) {
        MultiDataSourceProperties.DataSourceConfig dbConfig = properties.getDatabases().get(dbType.getCode());

        String url;
        String username;
        String password;

        if (dbConfig != null && dbConfig.getUrl() != null) {
            url = dbConfig.getUrl();
            username = dbConfig.getUsername() != null ? dbConfig.getUsername() : defaultUsername;
            password = dbConfig.getPassword() != null ? dbConfig.getPassword() : defaultPassword;
        } else {
            url = buildDatabaseUrl(defaultUrl, dbType.getCode());
            username = defaultUsername;
            password = defaultPassword;
        }

        return createHikariDataSource(url, username, password, defaultDriverClassName, dbType.getCode());
    }

    /**
     * 构建数据库URL
     * 将原始URL中的数据库名替换为指定的数据库名
     *
     * @param originalUrl 原始URL
     * @param databaseName 数据库名
     * @return 新的数据库URL
     */
    private String buildDatabaseUrl(String originalUrl, String databaseName) {
        int lastSlashIndex = originalUrl.lastIndexOf('/');
        int questionMarkIndex = originalUrl.indexOf('?', lastSlashIndex);

        if (questionMarkIndex > 0) {
            return originalUrl.substring(0, lastSlashIndex + 1) + databaseName + originalUrl.substring(questionMarkIndex);
        } else {
            return originalUrl.substring(0, lastSlashIndex + 1) + databaseName;
        }
    }

    /**
     * 创建HikariCP数据源
     *
     * @param url      数据库URL
     * @param username 用户名
     * @param password 密码
     * @param driverClassName 驱动类名
     * @param poolName 连接池名称
     * @return HikariCP数据源
     */
    private HikariDataSource createHikariDataSource(String url, String username, String password,
                                                     String driverClassName, String poolName) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(url);
        config.setUsername(username);
        config.setPassword(password);
        config.setDriverClassName(driverClassName);
        config.setPoolName("hikari-" + poolName);

        config.setMinimumIdle(minimumIdle);
        config.setMaximumPoolSize(maximumPoolSize);
        config.setIdleTimeout(idleTimeout);
        config.setConnectionTimeout(connectionTimeout);
        config.setMaxLifetime(maxLifetime);
        config.setConnectionTestQuery(connectionTestQuery);

        config.setAutoCommit(false);
        
        if (driverClassName != null && driverClassName.contains("mysql")) {
            config.setConnectionInitSql("SET NAMES utf8mb4");
        }
        
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        config.addDataSourceProperty("useServerPrepStmts", "true");
        config.addDataSourceProperty("useLocalSessionState", "true");
        config.addDataSourceProperty("rewriteBatchedStatements", "true");
        config.addDataSourceProperty("cacheResultSetMetadata", "true");
        config.addDataSourceProperty("cacheServerConfiguration", "true");
        config.addDataSourceProperty("elideSetAutoCommits", "true");
        config.addDataSourceProperty("maintainTimeStats", "false");

        return new HikariDataSource(config);
    }

    /**
     * 配置事务管理器
     * 使用动态路由数据源作为事务管理的数据源
     *
     * @param dynamicDataSource 动态路由数据源
     * @return 事务管理器
     */
    @Bean
    @Primary
    public PlatformTransactionManager transactionManager(DataSource dynamicDataSource) {
        return new DataSourceTransactionManager(dynamicDataSource);
    }
}
