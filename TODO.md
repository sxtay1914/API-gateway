# TODO

## One-Week Finalisation Goal

Finish a reliable, explainable portfolio version of the API gateway. Prioritise proving the existing request pipeline over adding new infrastructure.

**Definition of done:**

- [ ] The Maven test suite passes consistently
- [ ] JWT, routing, rate-limiting, and downstream failure behaviour are tested
- [ ] The proxy preserves the essential HTTP request and response data
- [ ] Docker Compose provides a working end-to-end environment
- [ ] GitHub Actions runs a successful smoke test
- [ ] No private keys or real secrets are stored in the repository
- [ ] Documentation accurately explains the architecture, setup, decisions, and limitations

## Day 1 — Stabilise the Test Harness

- [x] Fix the Testcontainers lifecycle and ensure containers stop after the suite
- [x] Make WireMock ports dynamic and pass their addresses through test properties/data
- [x] Make the JWT minting helper use its `sub` argument
- [x] Reset WireMock and Redis state between tests where necessary
- [x] Prevent WireMock configuration from affecting unrelated context tests
- [x] Remove duplicate or incorrectly scoped test dependencies
- [x] Get the existing JWT happy-path integration test passing
- [x] Explain the lifecycle of PostgreSQL, Redis, the JWKS server, and the downstream mock

## Day 2 — Authentication Behaviour

- [x] Decide and document the contract for missing and malformed credentials (`401` versus `400`)
- [x] Test an expired JWT and verify the downstream is not called
- [x] Test a token signed by an untrusted key
- [x] Test a malformed token
- [x] Test a missing `Authorization` header
- [x] Test a malformed Bearer header
- [x] Test actuator and public-path bypass behaviour
- [x] Validate the expected issuer and audience, or document why they are currently deferred
- [x] Explain the difference between parsing a JWT and trusting its claims

## Day 3 — Routing and Proxy Correctness

- [x] Test an existing path with the correct HTTP method
- [x] Test an unknown path returning `404`
- [x] Test an existing path with the wrong HTTP method (Should return 405)
- [x] Preserve query parameters when forwarding
- [x] Preserve relevant request headers
- [x] Forward request bodies for methods such as POST and PUT
- [x] Preserve downstream response status, headers, content type, and body
- [ ] Avoid unnecessarily buffering the complete downstream response (Deferred)
- [x] Test that a downstream error produces the intended gateway response
- [x] Explain what makes a proxy transparent

## Day 4 — Rate Limiting and Failure Behaviour

- [x] Test that requests within the route limit succeed
- [x] Test that the next request returns `429`
- [x] Verify rejected requests never reach the downstream service
- [x] Test isolation between different clients
- [x] Test isolation between different routes
- [x] Add expiry for inactive Redis rate-limit keys
- [x] Review the Lua window-boundary behaviour
- [x] Decide and document whether Redis failure should fail open or fail closed
- [x] Add rate-limit response headers if time permits
- [x] Explain why the Lua operation is atomic

## Day 5 — Build, Security, and Configuration Cleanup

- [ ] Align the documented and configured Java version
- [ ] Align the documented and configured Spring Boot version
- [ ] Remove the duplicate PostgreSQL dependency
- [ ] Remove multiple SLF4J provider bindings and verify the intended logger is active
- [ ] Review whether both imperative and reactive Redis starters are required
- [ ] Move WireMock/JWT test libraries into the appropriate test scope
- [ ] Replace hardcoded service settings with environment-driven configuration
- [ ] Restrict exposed actuator endpoints
- [ ] Remove committed private keys and rotate/recreate development keys
- [ ] Validate route destinations to reduce SSRF and routing-loop risk

## Day 6 — Docker, CI, and Performance Smoke Test

- [ ] Ensure the Compose stack includes a working authentication/JWKS service
- [ ] Ensure all health checks represent real readiness
- [ ] Remove duplicate seeder execution
- [ ] Align k6 and GitHub Actions environment-variable names
- [ ] Align generated report paths with uploaded artifact paths
- [ ] Run a successful k6 smoke scenario before attempting a stress scenario
- [ ] Confirm k6 checks validate successful gateway behaviour
- [ ] Save a fresh report and remove or label stale failed results
- [ ] Run the complete workflow from a clean checkout if possible

## Day 7 — Documentation and Interview Readiness

- [ ] Update `PROJECT_CONTEXT.md` to reflect completed features and the actual stack
- [ ] Replace or expand the generated `HELP.md` with useful setup instructions
- [ ] Document the request lifecycle from client to downstream service
- [ ] Add an architecture diagram
- [ ] Document the route-table schema and rate-limit algorithm
- [ ] Document why resilience is handled by Envoy rather than Resilience4j, or revise that decision
- [ ] Document how to start the stack, obtain a token, make a request, and run tests
- [ ] Document known limitations instead of hiding unfinished production concerns
- [ ] Practise explaining each filter and service without looking at the code
- [ ] Practise rebuilding the request flow from memory

## Completed Foundation

### WebClient Configuration

- [x] Configure connection timeout
- [x] Configure read and write timeouts
- [x] Configure response timeout
- [x] Configure the connection pool
- [x] Wire the Reactor Netty client into `WebClient`

### Dynamic Routing

- [x] Set up PostgreSQL and R2DBC
- [x] Create the route entity and composite route identifier
- [x] Create the reactive repository and routing service
- [x] Route all request methods through a functional WebFlux handler
- [x] Forward requests to a database-selected destination

### Authentication and Request Pipeline

- [x] Implement a correlation-ID filter
- [x] Implement Bearer-token extraction
- [x] Verify RS256 tokens through a remote JWKS endpoint
- [x] Pass the verified client identity through the exchange
- [x] Create a local FastAPI JWT/JWKS development server

### Rate Limiting and Infrastructure

- [x] Implement Redis-backed rate limiting with a Lua script
- [x] Configure Prometheus metrics exposure
- [x] Provision a Grafana Prometheus datasource
- [x] Add Envoy retries, circuit breaking, and outlier detection
- [x] Add PostgreSQL, Redis, Envoy, nginx, monitoring, and seeding services to Docker Compose
- [x] Create k6 smoke/stress scenarios and an HTML summary generator
- [x] Create an initial GitHub Actions performance-test workflow

## Deferred Until After the One-Week Finish

- [ ] Kafka event streaming
- [ ] Additional cloud infrastructure
- [ ] Advanced route caching and invalidation
- [ ] High-availability deployment
- [ ] Full production TLS and secret-management design
- [ ] Additional dashboards beyond the metrics needed to demonstrate the gateway

These are valuable extensions, but they should not delay a correct, tested, and explainable portfolio release.
