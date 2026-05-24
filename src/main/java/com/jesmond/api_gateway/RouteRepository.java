package com.jesmond.api_gateway;
  
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;
import org.springframework.data.r2dbc.repository.Query;

public interface RouteRepository extends ReactiveCrudRepository<RouteEntity, RouteId>{

  // can remove entirely because ReactiveCrudRepository provides findById by default
  // Mono<RouteEntity> findById(String path, HttpMethod method);
}
