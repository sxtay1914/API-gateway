package com.jesmond.api_gateway;

import org.springframework.http.HttpStatusCode;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;

import java.net.URI;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class RouteHandler {
  private WebClient webClient;
  private static final Logger logger = LoggerFactory.getLogger(RouteHandler.class);
  private HttpHeaders outgoingHeaders;

  public RouteHandler(WebClient webClient, WebClientConfig webClientConfig) {
    this.webClient = webClient;
    this.outgoingHeaders = new HttpHeaders();
  }

  public Mono<ServerResponse> forwardToDest(ServerRequest request) {
    Set<String> headersToRemove = Set.of("authorization", "content-length", "connection", "keep-alive",
        "proxy-authenticate", "proxy-authorization", "trailer", "transfer-encoding", "upgrade", "host");

    MultiValueMap<String, String> allRequestParams = request.exchange().getRequest().getQueryParams();

    UriComponentsBuilder downstreamServerUriBuilder = UriComponentsBuilder
        .fromUriString(request.exchange().getAttribute("routeDest"))
        .path(request.path()).queryParams(allRequestParams);
    URI downstreamServerUri = downstreamServerUriBuilder.build().toUri();
    String downstreamServerUriString = downstreamServerUriBuilder.toUriString();
    logger.info("downstreamServerURI: " + downstreamServerUriString);
    String correlationId = request.exchange().getAttribute("correlationId");

    String clientId = request.exchange().getAttribute("clientID");

    HttpHeaders reqHeaders = request.exchange().getRequest().getHeaders();
    outgoingHeaders.addAll(reqHeaders);
    headersToRemove.forEach(h -> {
      outgoingHeaders.remove(h);
    });
    // using flatMap to get String value out of Mono
    return webClient.method(request.method())
        .uri(downstreamServerUriString)
        .header("host", downstreamServerUri.getHost() + ":" + downstreamServerUri.getPort())
        .header("x-client-id", clientId)
        .headers(headers -> headers.addAll(outgoingHeaders))
        .retrieve()
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
