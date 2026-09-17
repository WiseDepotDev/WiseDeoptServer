package com.huicang.wise.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Redis配置属性
 *
 * @author WiseDepot
 * @version 0.0.27
 * @since 2026-02-27
 */
@ConfigurationProperties(prefix = "wise.redis")
public class RedisProperties {

    /**
     * Redis主机地址
     */
    private String host = "10.0.0.4";

    /**
     * Redis端口
     */
    private int port = 6379;

    /**
     * Redis密码
     */
    private String password = "Key-1122";

    /**
     * Redis数据库索引
     */
    private int database = 0;

    /**
     * 连接超时时间（秒）
     */
    private int timeout = 5;

    /**
     * 连接池最大连接数
     */
    private int maxActive = 8;

    /**
     * 连接池最大空闲连接数
     */
    private int maxIdle = 8;

    /**
     * 连接池最小空闲连接数
     */
    private int minIdle = 0;

    /**
     * 连接池最大等待时间（毫秒）
     */
    private long maxWait = -1;

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public int getDatabase() {
        return database;
    }

    public void setDatabase(int database) {
        this.database = database;
    }

    public int getTimeout() {
        return timeout;
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    public int getMaxActive() {
        return maxActive;
    }

    public void setMaxActive(int maxActive) {
        this.maxActive = maxActive;
    }

    public int getMaxIdle() {
        return maxIdle;
    }

    public void setMaxIdle(int maxIdle) {
        this.maxIdle = maxIdle;
    }

    public int getMinIdle() {
        return minIdle;
    }

    public void setMinIdle(int minIdle) {
        this.minIdle = minIdle;
    }

    public long getMaxWait() {
        return maxWait;
    }

    public void setMaxWait(long maxWait) {
        this.maxWait = maxWait;
    }
}
