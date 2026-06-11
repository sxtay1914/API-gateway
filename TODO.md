# TODO

## WebClient Configuration

- [x] Configure connection timeout via `HttpClient`
- [x] Configure read timeout via `HttpClient`
- [x] Configure response timeout via `HttpClient`
- [x] Configure connection pool (max connections, pending acquire queue)
- [x] Wire `HttpClient` into `WebClient` builder

## Dynamic Routing
- [x] Set up PostgreSQL
- [x] Configure R2DBC connection on application.yaml, need to add dependencies also
- [x] Create entity class
- [x] Create repo interface
- [x] Create routing service
- [x] Create RouteConfig (RouterFunction with RequestPredicates.all())
- [x] Create RouteHandler to intercept requests and forward via WebClient
- [x] Fix RouteConfig predicate — `contentType(APPLICATION_JSON)` blocks non-JSON requests; gateway should forward all content types
- [x] `RouteEntity.dest` should be `private`, not `public` — `@Getter` already handles access
- [x] `ResponseStatusException` in `RouteHandler.onStatus` uses `NOT_FOUND` but should be `BAD_GATEWAY`

- [] Debug BAD_GATEWAY error using curl

## Pre-session Prep
- [x] Read WebClient docs — trace get() → uri() → retrieve() → bodyToMono()
- [x] Understand Mono vs Flux and what subscribing means
- [x] Sketch the handler flow on paper before coding

## Neovim Setup
- [ ] Set up jdtls (Eclipse JDT Language Server) via nvim-lspconfig for Java auto-import and completion

## Up Next

- [ ] Dynamic routing from PostgreSQL
- [ ] JWT authentication filter
- [ ] Redis-based rate limiting
- [ ] Circuit breaking with Resilience4j
- [ ] Observability: Kafka, Prometheus, Grafana
