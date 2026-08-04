package com.jesmond.api_gateway;

import java.util.List;
import java.text.ParseException;
import java.util.Objects;
import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;

import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import reactor.core.publisher.Mono;

@Order(1)
@Component
public class JwtFilter implements WebFilter {

  private final JwtService jwtService;
  private final Set<String> noAuthPaths;
  @Value("${target_issuer}")
  private String targetIssuer;
  @Value("${target_audience}")
  private String targetAudience;
  private static final Logger logger = LoggerFactory.getLogger(JwtFilter.class);

  public JwtFilter(JwtService jwtService) {
    this.jwtService = jwtService;
    this.noAuthPaths = Set.of("/test");
  }

  @Override
  public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
    String correlationId = exchange.getAttribute("correlationId");
    if (exchange.getRequest().getPath().value().startsWith("/actuator")) {
      logger.info("[{}] Prometheus endpoint reached. JWT filter bypassed.", correlationId);
      return chain.filter(exchange);
    }

    for (String path : noAuthPaths) {
      if (exchange.getRequest().getPath().value().startsWith(path)) {
        logger.info("[{}] NoAuth path reached. JWT filter bypassed.", correlationId);
        return chain.filter(exchange);
      }
    }
    String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

    // Verify header format
    if (authHeader == null || !authHeader.startsWith("Bearer ")) {
      logger.error("[{}] Invalid authorization header.", correlationId);
      exchange.getResponse().setStatusCode(HttpStatus.BAD_REQUEST);
      return exchange.getResponse().setComplete();
    }
    String token = authHeader.substring(7);

    // Verify token first
    try {
      jwtService.verifyToken(token);
      logger.info("[{}] Token verified.", correlationId);
    } catch (Exception e) {
      logger.error("[{}] Invalid token. " + e, correlationId);
      exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
      return exchange.getResponse().setComplete();
    }
    // Parse the token afterwards
    try {
      // Get claims from token
      SignedJWT jwt = SignedJWT.parse(token);
      // Get claimset
      JWTClaimsSet claims = jwt.getJWTClaimsSet();
      logger.info("[{}] Claim parsed", correlationId);

      String issuer = claims.getIssuer();
      logger.info("[{}] Issuer: " + issuer, correlationId);
      List<String> audiences = claims.getAudience();
      String audience = "";
      for (String a : audiences) {
        if (a.equals(targetAudience)) {
          audience = a;
          break;
        }
      }
      logger.info("[{}] Audience: " + audience, correlationId);

      logger.info("[{}] Issuer and audience parsed from claim", correlationId);
      if (!Objects.equals(issuer, targetIssuer) || !Objects.equals(audience, targetAudience)) {
        throw new ParseException("Invalid audience or issuer", 2);
      }

      // Pass into exchange attributes
      exchange.getAttributes().put("clientID", claims.getSubject());
      return chain.filter(exchange);
    } catch (ParseException e) {
      logger.error("[{}] JWT parse error " + e, correlationId);
      exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
      return exchange.getResponse().setComplete();
    }
  }
}
