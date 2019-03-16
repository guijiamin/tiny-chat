package com.study.signalproxy.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import redis.clients.jedis.JedisPoolConfig;

import javax.annotation.Resource;

/**
 * Decription
 * <p>
 * </p>
 * DATE 2019/3/9.
 *
 * @author guijiamin.
 */
@Slf4j
@Configuration
@ConditionalOnProperty(name = "redis.cluster.enable", havingValue = "false")
public class RedisConfig {
    @Resource
    private RedisProperties redisProperties;

    @Bean(name = "rd")
    public RedisTemplate<String, String> getRedisTemplate() {
        log.info("Init redistemplate, host: {}, port: {}, database: {}...", redisProperties.getHost(), redisProperties.getPort(), redisProperties.getDatabase());
        StringRedisTemplate stringRedisTemplate = new StringRedisTemplate(getRedisConnectionFactory());
        stringRedisTemplate.opsForValue().set("test", "test");
        System.out.println("test=" + stringRedisTemplate.opsForValue().get("test"));
        log.info("Init redistemplate success...");
        return stringRedisTemplate;
    }

    @Bean
    public RedisConnectionFactory getRedisConnectionFactory() {
        JedisConnectionFactory connectionFactory = new JedisConnectionFactory();
        connectionFactory.setHostName(redisProperties.getHost());
        connectionFactory.setPort(redisProperties.getPort());
        connectionFactory.setDatabase(redisProperties.getDatabase());
        connectionFactory.setPoolConfig(getPoolConfig());//设置池配置
        return connectionFactory;
    }

    private JedisPoolConfig getPoolConfig() {
        JedisPoolConfig poolConfig = new JedisPoolConfig();
        poolConfig.setMaxIdle(200);
        poolConfig.setMaxTotal(1024);
        poolConfig.setMaxWaitMillis(10000);
        poolConfig.setTestOnBorrow(true);
        return poolConfig;
    }
}
