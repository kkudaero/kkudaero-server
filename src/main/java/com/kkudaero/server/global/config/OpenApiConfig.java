package com.kkudaero.server.global.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.boot.info.BuildProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    private static final String TITLE = "꾸대로 API";
    private static final String DESCRIPTION = "꾸대로 웹과 iOS가 함께 쓰는 서버 API. 계산 결과는 모두 예상치입니다.";

    @Bean
    public OpenAPI openApi(BuildProperties buildProperties) {
        return new OpenAPI().info(new Info()
                .title(TITLE)
                .description(DESCRIPTION)
                .version(buildProperties.getVersion()));
    }
}
