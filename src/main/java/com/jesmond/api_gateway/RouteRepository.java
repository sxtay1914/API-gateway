package com.jesmond.api_gateway;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;

public interface RouteRepository extends ReactiveCrudRepository<RouteEntity, RouteId> {
}
