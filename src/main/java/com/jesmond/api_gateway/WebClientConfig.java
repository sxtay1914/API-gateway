package com.jesmond.api-gateway;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {
  // set baseUrl, defaultHeader 
  @Bean
  public WebClient webClient(){
    return WebClient.builder()
            .baseUrl("https://api.example.com")
            .defaultHeader(HttpHeaders.CONTET_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .build();
  }
}
