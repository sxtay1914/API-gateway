package com.jesmond.api_gateway;

import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import java.nio.charset.StandardCharsets;

import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebExceptionHandler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import reactor.core.publisher.Mono;

@Component
@Order(-2)
public class CustomWebExceptionHandler implements WebExceptionHandler {
  /*
   * Default web handler will strip away Allow header from response for HttpStatus
   * 405, thus a custom webExceptionHandler is needed
   */
  private static final Logger logger = LoggerFactory.getLogger(RoutingService.class);

  @Override
  public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
    ServerHttpResponse response = exchange.getResponse();

    if (ex instanceof CustomMethodNotAllowedException) {
      logger.error("Custom webExceptionHandler executed");
      CustomMethodNotAllowedException customEx = (CustomMethodNotAllowedException) ex;
      HttpStatus status = HttpStatus.METHOD_NOT_ALLOWED;
      String errMsg = ex.getMessage();
      response.setStatusCode(status);
      response.getHeaders().add("Allow", customEx.getHeaderValue());
      String jsonResponse = String.format("{\"error\":\"%s\", \"status\":%s", errMsg, status);
      DataBuffer buffer = response.bufferFactory().wrap(jsonResponse.getBytes(StandardCharsets.UTF_8));
      return response.writeWith(Mono.just(buffer));
    }

    return Mono.error(ex);
  }
}
