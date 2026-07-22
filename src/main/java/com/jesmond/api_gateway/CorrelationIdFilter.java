package com.jesmond.api_gateway;

import java.util.UUID;

import org.slf4j.Logger;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;

import org.slf4j.LoggerFactory;

import reactor.core.publisher.Mono;

/**
 * Lightweight filter to obtain request id and pass it down for logging
 * purposes. Sits in front of JWT and rate limit
 * filters.
 */
@Component
@Order(0)
public class CorrelationIdFilter implements WebFilter {
  private static final Logger logger = LoggerFactory.getLogger(CorrelationIdFilter.class);

  @Override
  public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
    // Obtain the requestID
    String requestId = exchange.getRequest().getHeaders().getFirst("X-Request-Id");
    if (requestId == null) {
      requestId = UUID.randomUUID().toString();
    }

    logger.info("[{}] Correlation filter reached", requestId);
    exchange.getAttributes().put("correlationId", requestId);
    // Put requestID in response header
    exchange.getResponse().getHeaders().add("X-Correlation-Id", requestId);
    return chain.filter(exchange);
  }
}
