package com.huicang.wise.infrastructure.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.ServletContextInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;

@Configuration
@Profile("prod")
public class SecurityConfig {

    @Value("${server.ssl.enabled:false}")
    private boolean sslEnabled;

    @Value("${security.require-https:true}")
    private boolean requireHttps;

    @Bean
    public ServletContextInitializer servletContextInitializer() {
        return new ServletContextInitializer() {
            @Override
            public void onStartup(ServletContext servletContext) throws ServletException {
                if (requireHttps && sslEnabled) {
                    servletContext.setSessionTrackingModes(
                        java.util.Collections.singleton(jakarta.servlet.SessionTrackingMode.SSL)
                    );
                }
            }
        };
    }
}
