package com.jesmond.api_gateway;

import org.springframework.web.reactive.function.server.RequestPredicates;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RouteConfig {
  @Bean
  public RouterFunction<ServerResponse> reqRoutes(RouteHandler routeHandler) {
    return RouterFunctions.route(
        // route all methods
        RequestPredicates.all(),
        routeHandler::forwardToDest);
  }
}
