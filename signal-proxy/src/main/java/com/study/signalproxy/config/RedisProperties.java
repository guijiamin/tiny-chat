package com.study.signalproxy.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Decription
 * <p>
 * </p>
 * DATE 2019/3/9.
 *
 * @author guijiamin.
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "redis")
public class RedisProperties {
    private String host;
    private Integer port;
    private Integer database;

    private String nodes;
    private Integer timeout;
}
