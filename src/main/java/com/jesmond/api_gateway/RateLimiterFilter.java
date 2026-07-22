package com.jesmond.api_gateway;

import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import org.slf4j.LoggerFactory;

import reactor.core.publisher.Mono;

@Order(2)
@Component
public class RateLimiterFilter implements WebFilter {
  private RateLimiterService rateLimiterService;
  private RoutingService routingService;
  private static final Logger logger = LoggerFactory.getLogger(RateLimiterFilter.class);
  @Value("${segmentSizeMs}")
  private int segmentSizeMs;
  @Value("${windowSizeMs}")
  private int windowSizeMs;
  @Value("${gateway_url}")
  private String gatewayURL;

  public RateLimiterFilter(RateLimiterService rateLimiterService, RoutingService routingService) {
    this.rateLimiterService = rateLimiterService;
    this.routingService = routingService;
  }

  @Override
  public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
    String correlationId = exchange.getAttribute("correlationId");
    String destinationURI = exchange.getRequest().getURI().getPath();

    if (exchange.getRequest().getURI().getPath().contains("/actuator")) {
      logger.info("[{}] Prometheus endpoint hit. Rate limiter filter bypassed.", correlationId);
      exchange.getAttributes().put("routeDest", gatewayURL);
      return chain.filter(exchange);
    }
    Mono<RouteEntity> queryResponse = routingService.query(destinationURI,
        exchange.getRequest().getMethod(), correlationId);

    String clientId = exchange.getAttribute("clientID");
    String routeKey = exchange.getRequest().getURI().getPath();
    return queryResponse.flatMap(entity -> {
      exchange.getAttributes().put("routeDest", entity.getDest());
      logger.info("[{}] Rate limiter service allow request is called", correlationId);
      return rateLimiterService
          .allowRequest(segmentSizeMs, windowSizeMs, entity.getLimit(), clientId, routeKey, correlationId)
          .then(chain.filter(exchange));
    });
  }
}
