package com.kean;

import com.kean.config.DotEnvLoader;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication(exclude = {UserDetailsServiceAutoConfiguration.class})
@MapperScan("com.kean.mapper")
@ConfigurationPropertiesScan("com.kean.config")
@EnableScheduling
public class KeanApplication {

    public static void main(String[] args) {
        DotEnvLoader.load();
        SpringApplication.run(KeanApplication.class, args);
    }
}
