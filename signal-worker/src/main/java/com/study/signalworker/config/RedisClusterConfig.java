package com.study.signalworker.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.MapPropertySource;
import org.springframework.data.redis.connection.RedisClusterConfiguration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;

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
@ConditionalOnProperty(name = "redis.cluster.enable", havingValue = "true")
public class RedisClusterConfig {
    @Resource
    private RedisProperties redisProperties;

    @Bean
    public RedisTemplate<String, String> getRedisTemplate() {
        log.info("Init cluster redistemplate, nodes: {}, timeout: {}...", redisProperties.getNodes(), redisProperties.getTimeout());
        StringRedisTemplate stringRedisTemplate = new StringRedisTemplate(getRedisConnectionFactory());
        stringRedisTemplate.opsForValue().set("test", "test");
        System.out.println("test=" + stringRedisTemplate.opsForValue().get("test"));
        log.info("Init cluster redistemplate success...");
        return stringRedisTemplate;
    }

    @Bean
    public RedisConnectionFactory getRedisConnectionFactory() {
        Map<String, Object> source = new HashMap<>();
        source.put("spring.redis.cluster.nodes", redisProperties.getNodes());
        source.put("spring.redis.cluster.timeout", redisProperties.getTimeout());

        RedisClusterConfiguration redisClusterConfiguration = new RedisClusterConfiguration(new MapPropertySource("RedisClusterConfiguration", source));
        return new JedisConnectionFactory(redisClusterConfiguration);
    }
}
