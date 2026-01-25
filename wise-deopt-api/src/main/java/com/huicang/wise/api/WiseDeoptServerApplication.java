package com.huicang.wise.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;


/**
 * 类功能描述：应用启动入口
 *
 * @author xingchentye
 * @date 2026-01-19
 * @modified xingchentye 2026-01-19 创建Spring Boot启动类
 */
@SpringBootApplication(scanBasePackages = "com.huicang.wise")
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
