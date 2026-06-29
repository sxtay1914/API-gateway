package com.jesmond.api_gateway;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import org.springframework.core.io.Resource;

@Configuration
public class RedisConfig {
  @Bean
  public ReactiveRedisTemplate<String, Integer> reactiveRedisTemplate(
      ReactiveRedisConnectionFactory connectionFactory) {
    StringRedisSerializer serializer = new StringRedisSerializer();

    ReactiveRedisTemplate<String, Integer> redisTemplate;
    RedisSerializationContext<String, Integer> context = RedisSerializationContext
        .<String, Integer>newSerializationContext(serializer).build();

    redisTemplate = new ReactiveRedisTemplate<>(connectionFactory, context);
    return redisTemplate;
  }

  @Bean
  public RedisScript<Integer> checkAndSetScript() {
    System.out.println("Script Executed");
    DefaultRedisScript<Integer> script = new DefaultRedisScript<>();
    script.setLocation(new ClassPathResource("Redis/script.lua"));
    Resource resource = new ClassPathResource("Redis/script.lua");
    try {
      String content = resource.getContentAsString(StandardCharsets.UTF_8);

      // Read content as an InputStream (Great for large files)
      System.out.println(content);
    } catch (Exception e) {
      System.out.println(e);
    }
    script.setResultType(Integer.class);
    return script;
  }
}
