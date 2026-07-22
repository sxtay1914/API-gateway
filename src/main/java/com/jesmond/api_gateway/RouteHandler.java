package com.jesmond.api_gateway;

import org.springframework.http.HttpStatusCode;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class RouteHandler {
  private WebClient webClient;
  private static final Logger logger = LoggerFactory.getLogger(RouteHandler.class);

  public RouteHandler(WebClient webClient) {
    this.webClient = webClient;
  }

  public Mono<ServerResponse> forwardToDest(ServerRequest request) {
    String downstreamServerURL = request.exchange().getAttribute("routeDest") + request.path();
    String correlationId = request.exchange().getAttribute("correlationId");
    // using flatMap to get String value out of Mono
    return webClient.method(request.method()).uri(downstreamServerURL).retrieve()
        .onStatus(HttpStatusCode::isError,
            response -> response.bodyToMono(String.class).flatMap(
                errorBody -> {
                  return Mono.error(new ResponseStatusException(HttpStatus.BAD_GATEWAY, "BAD_GATEWAY " + errorBody));
                }))
        .bodyToMono(byte[].class)
        .doOnError(e -> logger.error("[{}] Route handler error: " + e, correlationId))
        .flatMap(bytes -> ServerResponse.ok().bodyValue(bytes));
  }
}
