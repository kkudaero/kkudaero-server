package com.kkudaero.server.global.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.boot.info.BuildProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    private static final String TITLE = "꾸대로 API";
    private static final String DESCRIPTION = "꾸대로 웹과 iOS가 함께 쓰는 서버 API. 계산 결과는 모두 예상치입니다.";
    private static final String SECURITY_SCHEME_NAME = "accessToken";
    private static final String BEARER_SCHEME = "bearer";
    private static final String BEARER_FORMAT = "JWT";

    @Bean
    public OpenAPI openApi(BuildProperties buildProperties) {
        SecurityScheme securityScheme = new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme(BEARER_SCHEME)
                .bearerFormat(BEARER_FORMAT)
                .description("로그인 API가 돌려준 accessToken");

        return new OpenAPI()
                .info(new Info()
                        .title(TITLE)
                        .description(DESCRIPTION)
                        .version(buildProperties.getVersion()))
                .components(new Components().addSecuritySchemes(SECURITY_SCHEME_NAME, securityScheme))
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME));
    }
}
