package com.huicang.wise.api.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Swagger/OpenAPI 配置
 *
 * @author xingchentye
 * @version 1.0
 */
@Configuration
public class SwaggerConfiguration {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .components(new Components()
                        .addSecuritySchemes("bearer-key",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList("bearer-key"))
                .servers(List.of(new Server().url("/").description("Default Server")))
                .info(new Info()
                        .title("Wise Deopt Server API")
                        .version("0.0.30")
                        .description("Wise Deopt Server API Documentation.\n\n" +
                                "版本：0.0.30 (2026-02-27)\n\n" +
                                "注意：所有请求和响应都使用统一的Packet格式包装。\n" +
                                "请求格式：\n" +
                                "```json\n" +
                                "{\n" +
                                "  \"header\": { ... },\n" +
                                "  \"body\": {\n" +
                                "    \"action\": \"...\",\n" +
                                "    \"payload\": { ... }\n" +
                                "  }\n" +
                                "}\n" +
                                "```\n" +
                                "响应格式同上，业务数据在 body.payload 中。"));
    }
}
