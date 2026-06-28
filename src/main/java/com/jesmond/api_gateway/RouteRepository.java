package com.jesmond.api_gateway;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;

public interface RouteRepository extends ReactiveCrudRepository<RouteEntity, RouteId> {

  // can remove entirely because ReactiveCrudRepository provides findById by
  // default
  // Mono<RouteEntity> findById(String path, HttpMethod method);

}
