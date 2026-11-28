package com.kkudaero.server.global.config;

import com.kkudaero.server.infra.unipass.UnipassProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.ClientHttpRequestFactorySettings;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(UnipassProperties.class)
public class UnipassConfig {

    @Bean
    public RestClient unipassRestClient(RestClient.Builder builder, UnipassProperties properties) {
        ClientHttpRequestFactorySettings settings = ClientHttpRequestFactorySettings.defaults()
                .withConnectTimeout(properties.connectTimeout())
                .withReadTimeout(properties.readTimeout());
        return builder.requestFactory(ClientHttpRequestFactoryBuilder.detect().build(settings)).build();
    }
}
