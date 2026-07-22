package com.jesmond.api_gateway;

import java.util.Collections;
import java.util.List;

import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import reactor.core.publisher.Mono;

@Service
public class RateLimiterService {
  private final ReactiveRedisTemplate<String, String> redisTemplate;
  private final RedisScript<Long> redisScript;
  private static final Logger logger = LoggerFactory.getLogger(JwtFilter.class);

  public RateLimiterService(ReactiveRedisTemplate<String, String> redisTemplate, RedisScript<Long> redisScript) {
    this.redisTemplate = redisTemplate;
    this.redisScript = redisScript;
  }

  public Mono<Void> allowRequest(int segmentSize, int windowSize, int limit, String client, String routeKey,
      String correlationId) {
    String redisKey = String.format("%s:%s", client, routeKey);
    List<String> key = Collections.singletonList(redisKey);
    long curr_seg = System.currentTimeMillis() / segmentSize;
    logger.info("[{}] Redis key: " + redisKey, correlationId);
    Mono<Long> result = redisTemplate.execute(redisScript,
        key,
        String.valueOf(curr_seg),
        String.valueOf(segmentSize),
        String.valueOf(windowSize),
        String.valueOf(limit)).next();

    return result.flatMap(r -> {
      logger.info("[{}] Allowed request " + r, correlationId);
      if (r == 0) {
        logger.warn("[{}] Rate limit hit", correlationId);
        return Mono.error(new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS));
      }
      return Mono.empty();
    });
  }
}
