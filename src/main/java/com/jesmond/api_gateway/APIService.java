package com.jesmond.api-gateway;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

@Service
public class ApiService {
  private final WebClient webClient;
  
  public ApiService(WebClient webClient){
    this.webClient = webClient;
  }

  public Mono<String> getData() {
    return webClient.get()
            .uri("/data")
            .retrieve()
            .bodyToMono(String.class);
  }

  public Mono<String> createData() {
    return webClient.post()
            .uri("/data")
            .retrieve()
            .bodyToMono(String.class);
  }
}
