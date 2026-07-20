package com.jesmond.api_gateway;

import java.text.ParseException;
import java.util.Set;

import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;

import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import reactor.core.publisher.Mono;

@Order(1)
@Component
public class JwtFilter implements WebFilter {

  private final JwtService jwtService;
  private final Set<String> noAuthPaths;

  public JwtFilter(JwtService jwtService) {
    this.jwtService = jwtService;
    this.noAuthPaths = Set.of("/test");
  }

  @Override
  public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
    if (exchange.getRequest().getPath().value().startsWith("/actuator")) {
      return chain.filter(exchange);
    }
    String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

    /*
     * Check for malformed header
     *
     */
    if (authHeader == null || !authHeader.startsWith("Bearer ")) {
      System.out.println("Check Executed");
      exchange.getResponse().setStatusCode(HttpStatus.BAD_REQUEST);
      return exchange.getResponse().setComplete();
    }
    String token = authHeader.substring(7);

    for (String path : noAuthPaths) {
      if (exchange.getRequest().getPath().value().startsWith(path)) {
        return chain.filter(exchange);
      }
    }

    // Verify token first
    try {
      jwtService.verifyToken(token);
      System.out.println("Filter Reached");
    } catch (Exception e) {
      System.err.println(e.getMessage());
      exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
      return exchange.getResponse().setComplete();
    }
    // Parse the token afterwards
    try {
      // Get claims from token
      SignedJWT jwt = SignedJWT.parse(token);
      // Get claimset
      JWTClaimsSet claims = jwt.getJWTClaimsSet();
      // Pass into exchange attributes
      exchange.getAttributes().put("clientID", claims.getSubject());
      return chain.filter(exchange);
    } catch (ParseException e) {
      System.err.println("JWT parse error " + e);
      exchange.getResponse().setStatusCode(HttpStatus.BAD_REQUEST);
      return exchange.getResponse().setComplete();
    }
  }
}
