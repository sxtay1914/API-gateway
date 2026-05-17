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
- [] Create routing service
- [] Create handler to intercept requests and forward via WebClient

## Up Next

- [ ] Dynamic routing from PostgreSQL
- [ ] JWT authentication filter
- [ ] Redis-based rate limiting
- [ ] Circuit breaking with Resilience4j
- [ ] Observability: Kafka, Prometheus, Grafana
