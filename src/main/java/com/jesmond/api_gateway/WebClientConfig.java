package com.jesmond.api_gateway;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import reactor.netty.http.client.HttpClient;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import reactor.netty.resources.ConnectionProvider;

@Configuration
public class WebClientConfig {
  @Bean
  public WebClient webClient(){
    // Configure connection pool
    ConnectionProvider provider = ConnectionProvider.builder("http-cp")
      .maxConnections(500)
      .pendingAcquireTimeout(Duration.ofSeconds(30))
      .pendingAcquireMaxCount(1000)
      .maxIdleTime(Duration.ofSeconds(20))
      .maxLifeTime(Duration.ofSeconds(60))
      .build();
    // Configured connection timeout, response timeout, read and write timeout
    HttpClient httpclient = HttpClient.create(provider)
      .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)
      .responseTimeout(Duration.ofSeconds(10))
      .doOnConnected(conn -> conn
          .addHandlerLast(new ReadTimeoutHandler(10, TimeUnit.SECONDS))
          .addHandlerLast(new WriteTimeoutHandler(10, TimeUnit.SECONDS))
      );
    return WebClient.builder()
            .clientConnector(new ReactorClientHttpConnector(httpclient))
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .build();
  }
}
