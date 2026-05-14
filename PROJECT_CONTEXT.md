# API Gateway — Project Context

## Project Overview

A production-ready API Gateway built as a portfolio project for backend engineering roles.

**Stack:** Spring Boot 3, Java 21, Spring WebFlux (reactive), PostgreSQL, Redis, Kafka, Resilience4j, Prometheus, Grafana  
**Deployment target:** VPS with HTTPS

## Planned Features

- Request proxying and forwarding
- Dynamic routing loaded from PostgreSQL
- JWT authentication
- Redis-based rate limiting
- Circuit breaking with Resilience4j
- Observability: Kafka event streaming, Prometheus metrics, Grafana dashboards

## Current Progress

- [x] Spring Boot project scaffolded with WebFlux and Actuator
- [ ] WebClient configuration class (in progress — the HTTP client for forwarding requests downstream)
- [ ] Dynamic routing from PostgreSQL
- [ ] JWT authentication filter
- [ ] Redis rate limiter
- [ ] Circuit breaker integration
- [ ] Observability pipeline

## Learning Philosophy

> Understand everything I write deeply. Do not generate code for me.

- Ask questions that lead to the answer
- Point to what to read, help think through tradeoffs
- Stress test understanding after implementation
- When stuck: ask what I've already tried before helping
- When I paste code I wrote: critique it and ask why I made each decision
- When a feature is done: interview me — make me explain it back as if rebuilding from scratch

## Side Goals

- Learning Neovim through this project
- Learning Linux through this project
- Fairly new to Spring Boot — learning by building
- Learn to use Cloud also