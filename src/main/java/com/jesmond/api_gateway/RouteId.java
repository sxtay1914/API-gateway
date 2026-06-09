package com.jesmond.api_gateway;

import org.springframework.http.HttpMethod;

public record RouteId(String path, HttpMethod method){}

