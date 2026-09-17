package com.huicang.wise.infrastructure.config;

import org.apache.catalina.connector.Connector;
import org.apache.coyote.http11.Http11NioProtocol;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("prod")
public class SslConfig {

    @Value("${server.ssl.enabled:false}")
    private boolean sslEnabled;

    @Value("${server.ssl.key-store}")
    private String keyStore;

    @Value("${server.ssl.key-store-password}")
    private String keyStorePassword;

    @Value("${server.ssl.key-alias}")
    private String keyAlias;

    @Bean
    public WebServerFactoryCustomizer<TomcatServletWebServerFactory> servletContainerCustomizer() {
        return factory -> {
            if (sslEnabled) {
                factory.addAdditionalTomcatConnectors(createStandardConnector());
                factory.addConnectorCustomizers(connector -> {
                    if (connector.getProtocolHandler() instanceof Http11NioProtocol) {
                        Http11NioProtocol protocol = (Http11NioProtocol) connector.getProtocolHandler();
                        
                        protocol.setSSLEnabled(true);
                        
                        protocol.setSecure(true);
                    }
                });
            }
        };
    }

    private Connector createStandardConnector() {
        Connector connector = new Connector("org.apache.coyote.http11.Http11NioProtocol");
        connector.setPort(8080);
        connector.setSecure(false);
        connector.setScheme("http");
        return connector;
    }
}
