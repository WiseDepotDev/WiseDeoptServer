package com.huicang.wise.api.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

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
                .components(new Components())
                .info(new Info()
                        .title("Wise Deopt Server API")
                        .version("1.0")
                        .description("Wise Deopt Server API Documentation.\n\n" +
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
