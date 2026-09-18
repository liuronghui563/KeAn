package com.kean.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "kean.jwt")
public class JwtProperties {

    /**
     * HS256 密钥，至少 32 字节。
     */
    private String secret;

    /**
     * Access Token 有效期（秒）。不实现 Refresh Token。
     */
    private long expireSeconds = 86400;
}
