package com.kean.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "kean.cors")
public class CorsProperties {

    private String allowedOrigins = "*";
}
