package com.kean.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "kean.admin")
public class AdminProperties {

    private String username = "admin";

    private String password;

    private String nickname = "系统管理员";
}
