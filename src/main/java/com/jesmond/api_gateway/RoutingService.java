package com.jesmond.api_gateway;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import org.springframework.http.HttpMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class RoutingService {
  private final RouteRepository routeRepository;
  private static final Logger logger = LoggerFactory.getLogger(RoutingService.class);

  public RoutingService(RouteRepository routeRepository) {
    this.routeRepository = routeRepository;
  }

  public Mono<RouteEntity> query(String path, HttpMethod method, String correlationId) {
    RouteId id = new RouteId(path, method.name());
    return routeRepository.findById(id)
        .switchIfEmpty(
            Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Destination Not Found")))
        .doOnError(e -> logger.error("[{}] Destination not found: " + e, correlationId));
  }
}
