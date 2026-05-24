package com.jesmond.api_gateway;

@Service
public class RoutingService{
  // Define repo
  private final RouteRepository routeRepository;

  public RoutingService(RouteRepository routeRepository){
    this.routeRepository = routeRepository;
  }

  public Mono<String> findDest(String path, HttpMethod method){
    RouteId id =  new RouteID(path, method);
    return routeRepository.findById(id).
      switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Destination Not Found"))).
      map(routeEntity -> {return routeEntity.dest;});
  }
}
