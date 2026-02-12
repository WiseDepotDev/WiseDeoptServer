package com.huicang.wise.api.config;

import com.huicang.wise.api.interceptor.AuthenticationInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

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
                        "/swagger-ui.html",     // 排除Swagger UI
                        "/swagger-ui/**",
                        "/v3/api-docs/**",
                        "/swagger-resources/**",
                        "/webjars/**"
                );
    }
}
