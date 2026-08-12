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

import io.netty.handler.timeout.ReadTimeoutException;
import io.netty.handler.timeout.WriteTimeoutException;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;

import java.net.ConnectException;
import java.net.SocketException;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.Set;

import javax.net.ssl.SSLException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class RouteHandler {
  private WebClient webClient;
  private static final Logger logger = LoggerFactory.getLogger(RouteHandler.class);
  private HttpHeaders outgoingHeaders;

  public RouteHandler(WebClient webClient, WebClientConfig webClientConfig) {
    this.webClient = webClient;
  }

  public Mono<ServerResponse> forwardToDest(ServerRequest request) {
    Set<String> headersToRemove = Set.of("authorization", "content-length", "connection", "keep-alive",
        "proxy-authenticate", "proxy-authorization", "trailer", "transfer-encoding", "upgrade", "host", "x-client-id");

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

    // cannot be stored as beans because this will leak headers between users
    this.outgoingHeaders = new HttpHeaders();

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
        .body(request.exchange().getRequest().getBody(), DataBuffer.class)
        .exchangeToMono(downstreamResponse -> {
          // split into 2 paths, if there is downstream response, remove several headers
          // and send it as-is to client to keep gateway transparent
          HttpStatusCode statusCode = downstreamResponse.statusCode();
          HttpHeaders headers = downstreamResponse.headers().asHttpHeaders();

          HttpHeaders gatewayHeaders = new HttpHeaders();
          gatewayHeaders.addAll(headers);
          System.out.println(downstreamResponse.headers().contentType());
          // Remove connection headers from downstream server response
          headersToRemove.forEach(header -> gatewayHeaders.remove(header));
          var responseBuilder = ServerResponse.status(statusCode).headers(header -> header.addAll(gatewayHeaders));

          if (downstreamResponse.headers().contentType().isPresent()) {
            responseBuilder.contentType(downstreamResponse.headers().contentType().get());
          }
          return responseBuilder.body(downstreamResponse.bodyToMono(byte[].class), byte[].class);
        })
        .onErrorResume(err -> {
          if (err instanceof ConnectException) {
            // Response timeout from downstream server
            logger.error("[{}] Downstream response timeout", correlationId);
            return Mono
                .error(new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Connection Refused: " + err.getMessage()));
          } else if (err instanceof UnknownHostException) {
            logger.error("[{}] DNS failed", correlationId);
            return Mono.error(
                new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Failed to resolve DNS " + err.getMessage()));
          } else if (err instanceof SSLException) {
            logger.error("[{}] TLS failure", correlationId);
            return Mono.error(new ResponseStatusException(HttpStatus.BAD_GATEWAY, "TLS failure " + err.getMessage()));
          } else if (err instanceof SocketException) {
            logger.error("[{}] Socket connection reset", correlationId);
            return Mono
                .error(new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Connection reset " + err.getMessage()));
          } else if (err instanceof ReadTimeoutException) {
            logger.error("[{}] Read timeout", correlationId);
            return Mono
                .error(new ResponseStatusException(HttpStatus.GATEWAY_TIMEOUT, "Read timeout " + err.getMessage()));
          } else if (err instanceof WriteTimeoutException) {
            logger.error("[{}] Write timeout", correlationId);
            return Mono
                .error(new ResponseStatusException(HttpStatus.GATEWAY_TIMEOUT, "Write timeout " + err.getMessage()));
          }
          logger.error("[{}] Internal Server Error", correlationId);
          return Mono
              .error(new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Bad gateway " + err.getMessage()));
        });
  }
}
