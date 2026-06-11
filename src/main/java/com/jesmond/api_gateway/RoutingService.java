package com.jesmond.api_gateway;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import org.springframework.http.HttpMethod;

@Service
public class RoutingService{
  // Define repo
  private final RouteRepository routeRepository;

  public RoutingService(RouteRepository routeRepository){
    this.routeRepository = routeRepository;
  }

  public Mono<String> findDest(String path, HttpMethod method){
    RouteId id =  new RouteId(path, method.name());
    return routeRepository.findById(id).
      switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Destination Not Found"))).
      map(RouteEntity::getDest);
  }
}
