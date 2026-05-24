package com.jesmond.api_gateway;

import org.springframework.http.HttpMethod;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Component 
public class RouteHandler{
  private RoutingService routingService;
  
  public Mono<ServerResponse> getDest(ServerRequest request){

  }  
} 
