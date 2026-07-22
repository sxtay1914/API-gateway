package com.jesmond.api_gateway;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.core.io.Resource;

@Configuration
public class RedisConfig {
  private static final Logger logger = LoggerFactory.getLogger(RedisConfig.class);

  @Primary
  @Bean
  public ReactiveRedisTemplate<String, String> reactiveRedisTemplate(
      ReactiveRedisConnectionFactory connectionFactory) {
    StringRedisSerializer serializer = new StringRedisSerializer();

    ReactiveRedisTemplate<String, String> redisTemplate;
    RedisSerializationContext<String, String> context = RedisSerializationContext
        .<String, String>newSerializationContext(serializer).build();

    redisTemplate = new ReactiveRedisTemplate<>(connectionFactory, context);
    return redisTemplate;
  }

  @Bean
  public RedisScript<Long> redisScript() {
    DefaultRedisScript<Long> script = new DefaultRedisScript<>();
    script.setLocation(new ClassPathResource("Redis/script.lua"));
    Resource resource = new ClassPathResource("Redis/script.lua");
    try {
      logger.info("Redis script executed.");
    } catch (Exception e) {
      logger.error("Redis script execution error: " + e);
    }
    script.setResultType(Long.class);
    return RedisScript.of(resource, Long.class);
  }
}
