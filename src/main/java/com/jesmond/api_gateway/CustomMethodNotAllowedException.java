package com.jesmond.api_gateway;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class CustomMethodNotAllowedException extends ResponseStatusException {
  private final String headerValue;

  public CustomMethodNotAllowedException(HttpStatus status, String reason, String headerValue) {
    super(status, reason);
    this.headerValue = headerValue;
  }

  @Override
  public HttpHeaders getHeaders() {
    HttpHeaders headers = new HttpHeaders();
    headers.add("Allow", this.headerValue);
    return headers;
  }

  public String getHeaderValue() {
    return this.headerValue;
  }
}
