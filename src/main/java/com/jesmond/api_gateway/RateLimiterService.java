package com.jesmond.api_gateway;

import java.util.Collections;

import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import reactor.core.publisher.Mono;

@Service
public class RateLimiterService {
  private final ReactiveRedisTemplate<String, Integer> redisTemplate;
  private final RedisScript<Integer> redisScript;

  public RateLimiterService(ReactiveRedisTemplate<String, Integer> redisTemplate, RedisScript<Integer> redisScript) {
    this.redisTemplate = redisTemplate;
    this.redisScript = redisScript;
  }

  public Mono<Void> allowRequest(int segmentSize, int windowSize, int limit, String client, String routeKey) {
    String redisKey = String.format("%s:%s", client, routeKey);
    long curr_seg = System.currentTimeMillis() / segmentSize;
    System.out.println(redisKey);
    Mono<Integer> result = redisTemplate.execute(redisScript,
        Collections.singletonList(redisKey),
        String.valueOf(curr_seg),
        String.valueOf(segmentSize),
        String.valueOf(windowSize),
        String.valueOf(limit)).doOnNext(v -> System.out.println("Hiii" + v))
        .doOnComplete(() -> System.out.println("Nothing here")).next();

    return result.flatMap(r -> {
      System.out.println("r");
      if (r == 0) {
        return Mono.error(new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS));
      }
      return Mono.empty();
    });
  }
}
