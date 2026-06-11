package com.jesmond.api_gateway;

import org.springframework.http.HttpStatusCode;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.http.HttpMethod;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

@Component 
public class RouteHandler{
  private RoutingService routingService;
  private WebClient webClient;
  
  public RouteHandler(RoutingService routingService, WebClient webClient){
    this.routingService = routingService;
    this.webClient = webClient;
  }

  public Mono<ServerResponse> forwardToDest(ServerRequest request){
    Mono<String> dest = routingService.findDest(request.path(), request.method());
    // using flatMap to get String value out of Mono
    return dest.flatMap(destURL -> webClient.method(request.method()).
      uri(destURL + request.path()).
      retrieve(). 
      onStatus(HttpStatusCode::isError, response -> 
          response.bodyToMono(String.class).
          flatMap(errorBody -> Mono.error(new ResponseStatusException(HttpStatus.BAD_GATEWAY, "BAD_GATEWAY " + errorBody)))).
      bodyToMono(byte[].class).
      flatMap(bytes -> ServerResponse.ok().bodyValue(bytes)));
  }  
}    
