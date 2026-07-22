package com.jesmond.api_gateway;

// Schema uses composite key (path, method)
public record RouteId(String path, String method) {
}
