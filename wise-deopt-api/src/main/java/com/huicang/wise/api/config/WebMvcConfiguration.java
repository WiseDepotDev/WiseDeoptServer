package com.huicang.wise.api.config;

import java.util.Locale;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver;

import com.huicang.wise.api.interceptor.AuthenticationInterceptor;

/**
 * 类功能描述：Web MVC配置，注册拦截器
 *
 * @author xingchentye
 * @version 1.0
 * @since 2026-02-07
 */
@Configuration
public class WebMvcConfiguration implements WebMvcConfigurer {

    private final AuthenticationInterceptor authenticationInterceptor;

    public WebMvcConfiguration(AuthenticationInterceptor authenticationInterceptor) {
        this.authenticationInterceptor = authenticationInterceptor;
    }

    /**
     * 方法功能描述：添加拦截器
     *
     * @param registry 拦截器注册表
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authenticationInterceptor)
                .addPathPatterns("/api/**") // 拦截所有API请求
                .excludePathPatterns(
                        "/api/auth/login",      // 排除登录接口
                        "/api/auth/nfc-login",  // 排除NFC登录接口
                        "/api/auth/nfc-pin-login", // 排除NFC+PIN登录接口
                        "/api/captcha/**",     // 排除验证码接口
                        "/api/device",          // 排除设备注册接口 (通过签名验证)
                        "/api/device/heartbeat", // 排除设备心跳接口 (通过签名验证)
                        "/api/device/config",   // 排除设备配置接口 (通过签名验证)
                        "/api/device/logs/upload", // 排除设备日志上传接口 (通过签名验证)
                        "/api/inspection/task", // 排除巡检任务查询接口 (设备端通过签名验证)
                        "/api/inspection/task/**", // 排除巡检任务操作接口 (设备端通过签名验证)
                        "/api/inventory/expected", // 排除预期库存接口 (设备端通过签名验证)
                        "/api/inventories/expected", // 排除预期库存接口 (设备端通过签名验证)
                        "/swagger-ui.html",     // 排除Swagger UI
                        "/swagger-ui/**",
                        "/v3/api-docs/**",
                        "/swagger-resources/**",
                        "/webjars/**"
                );
    }

    /**
     * 方法功能描述：配置AcceptHeaderLocaleResolver Bean
     *
     * @return AcceptHeaderLocaleResolver实例
     */
    @Bean
    public AcceptHeaderLocaleResolver localeResolver() {
        AcceptHeaderLocaleResolver resolver = new AcceptHeaderLocaleResolver();
        resolver.setDefaultLocale(Locale.SIMPLIFIED_CHINESE);
        return resolver;
    }
}
