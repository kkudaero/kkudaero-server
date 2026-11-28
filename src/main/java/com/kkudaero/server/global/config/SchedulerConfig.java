package com.kkudaero.server.global.config;

import com.kkudaero.server.domain.customs.service.CustomsPollProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
@EnableConfigurationProperties(CustomsPollProperties.class)
public class SchedulerConfig {
}
