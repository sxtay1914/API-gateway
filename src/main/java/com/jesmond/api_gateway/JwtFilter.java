package com.jesmond.api_gateway;

import org.springframework.http.HttpHeaders;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;

import reactor.core.publisher.Mono;

@Component
public class JwtFilter implements WebFilter {

  private final JwtService jwtService;

  public JwtFilter(JwtService jwtService) {
    this.jwtService = jwtService;
  }

  @Override
  public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
    String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

    /*
     * Check for malformed header
     *
     */
    if (authHeader == null || !authHeader.startsWith("Bearer ")) {
      // System.out.println("Check Executed")
      exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
      return exchange.getResponse().setComplete();
    }

    String token = authHeader.substring(7);

    try {
      jwtService.verifyToken(token);
      return chain.filter(exchange);
    } catch (Exception e) {
      exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
      return exchange.getResponse().setComplete();
    }
  }
}
