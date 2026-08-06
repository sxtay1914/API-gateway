package com.jesmond.api_gateway;

import org.springframework.http.HttpStatus;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.stereotype.Service;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import org.springframework.http.HttpMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class RoutingService {
  private final RouteRepository routeRepository;
  private static final Logger logger = LoggerFactory.getLogger(RoutingService.class);
  private final DatabaseClient databaseClient;

  public RoutingService(RouteRepository routeRepository, DatabaseClient databaseClient) {
    this.routeRepository = routeRepository;
    this.databaseClient = databaseClient;
  }

  public Mono<RouteEntity> query(String path, HttpMethod method, String correlationId) {
    RouteId id = new RouteId(path, method.name());
    return routeRepository.findById(id)
        .switchIfEmpty(
            Mono.defer(() -> {
              return queryAllowedMethodsForRoute(path, correlationId);
            }))
        .doOnError(e -> logger.error("[{}] DB query executed: " + e, correlationId));
  }

  private Mono<RouteEntity> queryAllowedMethodsForRoute(String path, String correlationId) {
    // Query all allowed methods for a particular path
    Flux<String> AllowedMethods = this.databaseClient.sql("SELECT method FROM routes WHERE path = :path")
        .bind("path", path)
        .map((row, metadata) -> (row.get("method", String.class)))
        .all();

    // Convert Flux<String> to Mono<String>
    Mono<String> concatenatedAllowedMethods = AllowedMethods.reduce((s1, s2) -> s1 + ", " + s2);

    return concatenatedAllowedMethods.flatMap((allowedMethod) -> {
      logger.error("[{}] Method not allowed ", correlationId);
      logger.error("[{}] Allowed method: " + allowedMethod, correlationId);
      return Mono.error(
          new CustomMethodNotAllowedException(HttpStatus.METHOD_NOT_ALLOWED, "Method Not Allowed", allowedMethod));
    }).switchIfEmpty(Mono.defer(
        () -> {
          logger.error("[{}] Destination not found ", correlationId);
          return Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Destination Not Found"));
        })).cast(RouteEntity.class);
  }
}
