package com.jesmond.api_gateway;

import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;

import reactor.core.publisher.Mono;

@Order(2)
@Component
public class RateLimiterFilter implements WebFilter {
  private RateLimiterService rateLimiterService;
  private RoutingService routingService;

  public RateLimiterFilter(RateLimiterService rateLimiterService, RoutingService routingService) {
    this.rateLimiterService = rateLimiterService;
    this.routingService = routingService;
  }

  @Override
  public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
    Mono<RouteEntity> queryResponse = routingService.query(exchange.getRequest().getURI().getPath(),
        exchange.getRequest().getMethod());

    String clientId = exchange.getAttribute("clientID");
    String routeKey = exchange.getRequest().getURI().getPath();
    int segmentSize = 1000;
    int windowSize = 6000;
    return queryResponse.flatMap(entity -> {
      exchange.getAttributes().put("routeEntity", entity);
      System.out.println("Allow Request Called");
      return rateLimiterService.allowRequest(segmentSize, windowSize, entity.getLimit(), clientId, routeKey)
          .then(chain.filter(exchange));
    });
  }
}
