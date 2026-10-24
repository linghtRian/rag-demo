package com.ai.ragdemo1.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "llm")
@Data
public class LlmConfig {
    private String apiKey;
    private String endpoint;
    private String model;
}
