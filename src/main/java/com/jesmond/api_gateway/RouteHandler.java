package com.jesmond.api_gateway;

import org.springframework.http.HttpStatusCode;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

@Component
public class RouteHandler {
  private WebClient webClient;

  public RouteHandler(WebClient webClient) {
    this.webClient = webClient;
  }

  public Mono<ServerResponse> forwardToDest(ServerRequest request) {
    RouteEntity routeEntity = request.exchange().getAttribute("routeEntity");
    // using flatMap to get String value out of Mono
    return webClient.method(request.method()).uri(routeEntity.getDest() + request.path()).retrieve()
        .onStatus(HttpStatusCode::isError, response -> response.bodyToMono(String.class).flatMap(
            errorBody -> Mono.error(new ResponseStatusException(HttpStatus.BAD_GATEWAY, "BAD_GATEWAY " + errorBody))))
        .bodyToMono(byte[].class).flatMap(bytes -> ServerResponse.ok().bodyValue(bytes));
  }
}
