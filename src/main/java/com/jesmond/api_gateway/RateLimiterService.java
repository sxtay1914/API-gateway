package com.jesmond.api_gateway;

import java.util.Collections;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class RateLimiterService {
  private final RedisTemplate<String, Object> redisTemplate;
  private final RedisScript<Boolean> redisScript;

  public RateLimiterService(RedisTemplate<String, Object> redisTemplate, RedisScript<Boolean> redisScript) {
    this.redisTemplate = redisTemplate;
    this.redisScript = redisScript;
  }

  public void allowRequest(int segmentSize, int windowSize, int limit, String client, String routeKey)
      throws ResponseStatusException {
    String redisKey = String.format("%s:%s", client, routeKey);
    long curr_seg = System.currentTimeMillis() / segmentSize;
    Boolean result = redisTemplate.execute(redisScript,
        Collections.singletonList(redisKey),
        String.valueOf(curr_seg),
        String.valueOf(segmentSize),
        String.valueOf(windowSize),
        String.valueOf(limit));

    if (!result) {
      throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Too many requests. Please try again later");
    }
  }
}
