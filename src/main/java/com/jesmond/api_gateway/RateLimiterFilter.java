package com.jesmond.api_gateway;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;

import reactor.core.publisher.Mono;

@Component
public class RateLimiterFilter implements WebFilter {
  private RateLimiterService rateLimiterService;

  public RateLimiterFilter(RateLimiterService rateLimiterService) {
    this.rateLimiterService = rateLimiterService;
  }

  @Override
  @RateLimit
  public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
    String clientId = exchange.getAttribute("clientId");
    String routeKey = exchange.getRequest().getURI().getPath();

    try {
      rateLimiterService.allowRequest(rateLimit, clientId, routeKey);
      chain.filter(exchange);
    } catch (Exception e) {
      exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
      exchange.getResponse().setComplete();
    }

  }
}
