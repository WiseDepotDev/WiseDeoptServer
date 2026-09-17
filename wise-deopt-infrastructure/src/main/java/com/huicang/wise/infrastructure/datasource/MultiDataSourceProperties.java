package com.huicang.wise.infrastructure.datasource;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashMap;
import java.util.Map;

/**
 * 多数据源配置属性
 * 从配置文件中读取各个数据库的连接信息
 *
 * @author WiseDepot
 * @version 0.0.21
 * @since 2026-02-27
 */
@ConfigurationProperties(prefix = "wise.datasource")
public class MultiDataSourceProperties {

    /**
     * 主数据库URL（用于默认连接）
     */
    private String url;

    /**
     * 主数据库用户名
     */
    private String username;

    /**
     * 主数据库密码
     */
    private String password;

    /**
     * 数据库驱动类名
     */
    private String driverClassName = "com.mysql.cj.jdbc.Driver";

    /**
     * 各数据库配置映射
     * key: 数据库名称（user, auth, inventory等）
     * value: 该数据库的配置
     */
    private Map<String, DataSourceConfig> databases = new HashMap<>();

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getDriverClassName() {
        return driverClassName;
    }

    public void setDriverClassName(String driverClassName) {
        this.driverClassName = driverClassName;
    }

    public Map<String, DataSourceConfig> getDatabases() {
        return databases;
    }

    public void setDatabases(Map<String, DataSourceConfig> databases) {
        this.databases = databases;
    }

    /**
     * 单个数据源配置
     */
    public static class DataSourceConfig {

        private String url;

        private String username;

        private String password;

        private String driverClassName;

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public String getDriverClassName() {
            return driverClassName;
        }

        public void setDriverClassName(String driverClassName) {
            this.driverClassName = driverClassName;
        }
    }
}
