package com.jesmond.api_gateway;

import org.junit.jupiter.api.Test;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.Options;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.Payload;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.sun.jndi.url.dns.dnsURLContextFactory;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.r2dbc.core.DatabaseClient;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Set;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@Testcontainers
public class IntegrationTest {
  // Define TestContainers for both Redis and Postgres
  @Container
  static PostgreSQLContainer<?> postgresTestContainer = new PostgreSQLContainer<>("postgres:16")
      .withDatabaseName("postgresTestContainer")
      .withUsername("root")
      .withPassword("root_password")
      .withInitScript("dbInit.sql");
  @Container
  static GenericContainer<?> redisTestContainer = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
      .withExposedPorts(6379);
  static final WireMockServer authMockServer = new WireMockServer(Options.DYNAMIC_PORT);
  static final WireMockServer downStreamServer = new WireMockServer(Options.DYNAMIC_PORT);
  static final RSAKey trustedSigningKey;
  static final RSAKey untrustedSigningKey;
  @Autowired
  private DatabaseClient databaseClient;
  @Autowired
  private WebTestClient webTestClient;
  @Autowired
  private ReactiveRedisTemplate<String, String> redisTemplate;

  private String mint(String sub, Duration ttl, RSAKey signingKey, String audience, String issuer) {
    // generate JWT token with JWT header and JWT claimsets
    try {
      Instant now = Instant.now();
      SignedJWT jwt = new SignedJWT(
          new JWSHeader.Builder(JWSAlgorithm.RS256)
              .keyID(signingKey.getKeyID())
              .build(),
          new JWTClaimsSet.Builder()
              .subject(sub)
              .issueTime(Date.from(now))
              .expirationTime(Date.from(now.plus(ttl)))
              .issuer(issuer)
              .audience(audience)
              .build());
      jwt.sign(new RSASSASigner(signingKey));
      return jwt.serialize();
    } catch (JOSEException e) {
      throw new IllegalStateException("JWT token failed to generate " + e);
    }
  }

  private String noAudienceMint(String sub, Duration ttl, RSAKey signingKey, String issuer) {
    // generate JWT token with JWT header and JWT claimsets
    try {
      Instant now = Instant.now();
      SignedJWT jwt = new SignedJWT(
          new JWSHeader.Builder(JWSAlgorithm.RS256)
              .keyID(signingKey.getKeyID())
              .build(),
          new JWTClaimsSet.Builder()
              .subject(sub)
              .issueTime(Date.from(now))
              .expirationTime(Date.from(now.plus(ttl)))
              .issuer(issuer)
              .build());
      jwt.sign(new RSASSASigner(signingKey));
      return jwt.serialize();
    } catch (JOSEException e) {
      throw new IllegalStateException("JWT token failed to generate " + e);
    }
  }

  private String noIssuerMint(String sub, Duration ttl, RSAKey signingKey, String audience) {
    // generate JWT token with JWT header and JWT claimsets
    try {
      Instant now = Instant.now();
      SignedJWT jwt = new SignedJWT(
          new JWSHeader.Builder(JWSAlgorithm.RS256)
              .keyID(signingKey.getKeyID())
              .build(),
          new JWTClaimsSet.Builder()
              .subject(sub)
              .issueTime(Date.from(now))
              .expirationTime(Date.from(now.plus(ttl)))
              .audience(audience)
              .build());
      jwt.sign(new RSASSASigner(signingKey));
      return jwt.serialize();
    } catch (JOSEException e) {
      throw new IllegalStateException("JWT token failed to generate " + e);
    }
  }

  static {
    authMockServer.start();
    try {
      // create signingKeys
      trustedSigningKey = new RSAKeyGenerator(2048).keyID("my-key-id").algorithm(JWSAlgorithm.RS256)
          .keyUse(KeyUse.SIGNATURE)
          .generate();

      untrustedSigningKey = new RSAKeyGenerator(2048).keyID("my-key-id").algorithm(JWSAlgorithm.RS256)
          .keyUse(KeyUse.SIGNATURE)
          .generate();

    } catch (JOSEException e) {
      throw new AssertionError("Failed to generate signing key" + e);
    }
    // create stub point for well-known/jwks.json
    authMockServer
        .stubFor(
            get("/.well-known/jwks.json").willReturn(okJson(new JWKSet(trustedSigningKey.toPublicJWK()).toString())));

    downStreamServer.start();
  }

  @AfterAll
  static void afterAll() {
    authMockServer.stop();
    downStreamServer.stop();
  }

  @BeforeEach
  void beforeEach() {
    // reset request history for authMockServer but retain stubs
    authMockServer.resetRequests();
    // reset request history and stubs for downstreamServer
    downStreamServer.resetAll();
    // delete all route from db
    var deleteMono = databaseClient.sql("DELETE FROM routes")
        .then();

    StepVerifier.create(deleteMono)
        .verifyComplete();

    // clear dataabse in redis
    var flushMono = redisTemplate.execute((connection) -> {
      return connection.serverCommands().flushDb();
    }).then();

    StepVerifier.create(flushMono)
        .verifyComplete();
  }

  @DynamicPropertySource
  static void configureProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.r2dbc.url", () -> "r2dbc:pool:postgresql://" + postgresTestContainer.getHost() + ":"
        + postgresTestContainer.getMappedPort(5432) + "/" + postgresTestContainer.getDatabaseName());
    registry.add("spring.r2dbc.username", postgresTestContainer::getUsername);
    registry.add("spring.r2dbc.password", postgresTestContainer::getPassword);
    registry.add("spring.data.redis.host", redisTestContainer::getHost);
    registry.add("spring.data.redis.port", redisTestContainer::getFirstMappedPort);
    registry.add("auth_server_url", () -> authMockServer.baseUrl() + "/.well-known/jwks.json");
  }

  @Test
  void validJwt() {
    // Insert downstreamServer record into sql
    var insertMono = databaseClient.sql("INSERT INTO routes VALUES(:path, :method, :downstreamServerURL, :rateLimit)")
        .bind("path", "/validJwt")
        .bind("method", "GET")
        .bind("downstreamServerURL", "http://localhost:" + Integer.toString(downStreamServer.port()))
        .bind("rateLimit", 999)
        .then();

    StepVerifier.create(insertMono)
        .verifyComplete();

    // create stub for downstream server
    downStreamServer.stubFor(get("/validJwt").willReturn(aResponse().withStatus(200)));

    this.webTestClient = webTestClient.mutate()
        .responseTimeout(Duration.ofSeconds(30))
        .defaultHeader(HttpHeaders.AUTHORIZATION,
            "Bearer " + mint("Springboot Test User", Duration.ofMinutes(5), trustedSigningKey, "api-gateway",
                "http://localhost:8000"))
        .build();

    // Hit api gateway endpoint
    // Assert 200 ok response received
    webTestClient.get()
        .uri("/validJwt")
        .exchange()
        .expectStatus()
        .isOk();

    downStreamServer
        .verify(exactly(1), getRequestedFor(urlEqualTo("/validJwt")));
  }

  @Test
  void wrongIssuerInClaims() {
    // Insert downstreamServer record into sql
    var insertMono = databaseClient.sql("INSERT INTO routes VALUES(:path, :method, :downstreamServerURL, :rateLimit)")
        .bind("path", "/wrongIssuer")
        .bind("method", "GET")
        .bind("downstreamServerURL", "http://localhost:" + Integer.toString(downStreamServer.port()))
        .bind("rateLimit", 999)
        .then();

    StepVerifier.create(insertMono)
        .verifyComplete();

    // create stub for downstream server
    downStreamServer.stubFor(get("/wrongIssuer").willReturn(aResponse().withStatus(200)));

    this.webTestClient = webTestClient.mutate()
        .responseTimeout(Duration.ofSeconds(30))
        .defaultHeader(HttpHeaders.AUTHORIZATION,
            "Bearer " + mint("Springboot Test User", Duration.ofMinutes(5), trustedSigningKey, "api-gateway",
                "wrong-issuer"))
        .build();

    // Hit api gateway endpoint
    webTestClient.get()
        .uri("/wrongIssuer")
        .exchange()
        .expectStatus()
        .isUnauthorized();

    downStreamServer
        .verify(exactly(0), getRequestedFor(urlEqualTo("/wrongIssuer")));
  }

  @Test
  void wrongAudienceInClaims() {
    // Insert downstreamServer record into sql
    var insertMono = databaseClient.sql("INSERT INTO routes VALUES(:path, :method, :downstreamServerURL, :rateLimit)")
        .bind("path", "/wrongAudience")
        .bind("method", "GET")
        .bind("downstreamServerURL", "http://localhost:" + Integer.toString(downStreamServer.port()))
        .bind("rateLimit", 999)
        .then();

    StepVerifier.create(insertMono)
        .verifyComplete();

    // create stub for downstream server
    downStreamServer.stubFor(get("/wrongAudience").willReturn(aResponse().withStatus(200)));

    this.webTestClient = webTestClient.mutate()
        .responseTimeout(Duration.ofSeconds(30))
        .defaultHeader(HttpHeaders.AUTHORIZATION,
            "Bearer " + mint("Springboot Test User", Duration.ofMinutes(5), trustedSigningKey, "wrong-audience",
                "http://localhost:8000"))
        .build();

    // Hit api gateway endpoint
    webTestClient.get()
        .uri("/wrongAudience")
        .exchange()
        .expectStatus()
        .isUnauthorized();

    downStreamServer
        .verify(exactly(0), getRequestedFor(urlEqualTo("/wrongAudience")));
  }

  @Test
  void noAudienceClaim() {
    // Insert downstreamServer record into sql
    var insertMono = databaseClient.sql("INSERT INTO routes VALUES(:path, :method, :downstreamServerURL, :rateLimit)")
        .bind("path", "/noAudienceClaim")
        .bind("method", "GET")
        .bind("downstreamServerURL", "http://localhost:" + Integer.toString(downStreamServer.port()))
        .bind("rateLimit", 999)
        .then();

    StepVerifier.create(insertMono)
        .verifyComplete();

    // create stub for downstream server
    downStreamServer.stubFor(get("/noAudienceClaim").willReturn(aResponse().withStatus(200)));

    this.webTestClient = webTestClient.mutate()
        .responseTimeout(Duration.ofSeconds(30))
        .defaultHeader(HttpHeaders.AUTHORIZATION,
            "Bearer " + noAudienceMint("Springboot Test User", Duration.ofMinutes(5), trustedSigningKey,
                "http://localhost:8000"))
        .build();

    // Hit api gateway endpoint
    webTestClient.get()
        .uri("/noAudienceClaim")
        .exchange()
        .expectStatus()
        .isUnauthorized();

    downStreamServer
        .verify(exactly(0), getRequestedFor(urlEqualTo("/noAudienceClaim")));
  }

  @Test
  void noIssuerClaim() {
    // Insert downstreamServer record into sql
    var insertMono = databaseClient.sql("INSERT INTO routes VALUES(:path, :method, :downstreamServerURL, :rateLimit)")
        .bind("path", "/noIssueClaim")
        .bind("method", "GET")
        .bind("downstreamServerURL", "http://localhost:" + Integer.toString(downStreamServer.port()))
        .bind("rateLimit", 999)
        .then();

    StepVerifier.create(insertMono)
        .verifyComplete();

    // create stub for downstream server
    downStreamServer.stubFor(get("/noIssueClaim").willReturn(aResponse().withStatus(200)));

    this.webTestClient = webTestClient.mutate()
        .responseTimeout(Duration.ofSeconds(30))
        .defaultHeader(HttpHeaders.AUTHORIZATION,
            "Bearer " + noIssuerMint("Springboot Test User", Duration.ofMinutes(5), trustedSigningKey, "api-gateway"))
        .build();

    // Hit api gateway endpoint
    webTestClient.get()
        .uri("/noIssueClaim")
        .exchange()
        .expectStatus()
        .isUnauthorized();

    downStreamServer
        .verify(exactly(0), getRequestedFor(urlEqualTo("/noIssueClaim")));
  }

  @Test
  void expiredJwt() {
    var insertMono = databaseClient.sql("INSERT INTO routes VALUES(:path, :method, :downstreamServerURL, :rateLimit)")
        .bind("path", "/expiredJwt")
        .bind("method", "GET")
        .bind("downstreamServerURL", "http://localhost:" + Integer.toString(downStreamServer.port()))
        .bind("rateLimit", 999)
        .then();

    StepVerifier.create(insertMono)
        .verifyComplete();

    downStreamServer.stubFor(get("/expiredJwt").willReturn(aResponse().withStatus(200)));

    // Build webTestClient with expired JWT
    this.webTestClient = webTestClient.mutate()
        .responseTimeout(Duration.ofSeconds(30))
        .defaultHeader(HttpHeaders.AUTHORIZATION,
            "Bearer " + mint("Springboot Test User", Duration.ofMinutes(-1), trustedSigningKey, "api-gateway",
                "http://localhost:8000"))
        .build();

    webTestClient.get()
        .uri("/expiredJwt")
        .exchange()
        .expectStatus()
        .isUnauthorized();

    downStreamServer
        .verify(exactly(0), getRequestedFor(urlEqualTo("/expiredJwt")));
  }

  @Test
  void tokenSignedByUntrustedKey() {
    var insertMono = databaseClient.sql("INSERT INTO routes VALUES(:path, :method, :downstreamServerURL, :rateLimit)")
        .bind("path", "/untrustedKey")
        .bind("method", "GET")
        .bind("downstreamServerURL", "http://localhost:" + Integer.toString(downStreamServer.port()))
        .bind("rateLimit", 999)
        .then();

    StepVerifier.create(insertMono)
        .verifyComplete();

    downStreamServer.stubFor(get("/untrustedKey").willReturn(aResponse().withStatus(200)));
    // Build webTestClient with expired JWT
    this.webTestClient = webTestClient.mutate()
        .responseTimeout(Duration.ofSeconds(30))
        .defaultHeader(HttpHeaders.AUTHORIZATION,
            "Bearer " + mint("Springboot Test User", Duration.ofMinutes(5), untrustedSigningKey, "api-gateway",
                "http://localhost:8000"))
        .build();

    webTestClient.get()
        .uri("/untrustedKey")
        .exchange()
        .expectStatus()
        .isUnauthorized();

    downStreamServer
        .verify(exactly(0), getRequestedFor(urlEqualTo("/untrustedKey")));
  }

  @Test
  void malformedToken() {
    String malformedJWT = "not-a-jwt";

    var insertMono = databaseClient.sql("INSERT INTO routes VALUES(:path, :method, :downstreamServerURL, :rateLimit)")
        .bind("path", "/malformedToken")
        .bind("method", "GET")
        .bind("downstreamServerURL", "http://localhost:" + Integer.toString(downStreamServer.port()))
        .bind("rateLimit", 999)
        .then();

    StepVerifier.create(insertMono)
        .verifyComplete();

    downStreamServer.stubFor(get("/malformedToken").willReturn(aResponse().withStatus(200)));

    this.webTestClient = webTestClient.mutate()
        .responseTimeout(Duration.ofSeconds(30))
        .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + malformedJWT)
        .build();

    webTestClient.get()
        .uri("/malformedToken")
        .exchange()
        .expectStatus()
        .isUnauthorized();

    downStreamServer.verify(exactly(0), getRequestedFor(urlEqualTo("/malformedToken")));
  }

  @Test
  void missingAuthorizationHeader() {
    var insertMono = databaseClient.sql("INSERT INTO routes VALUES(:path, :method, :downstreamServerURL, :rateLimit)")
        .bind("path", "/missingAuthorizationHeader")
        .bind("method", "GET")
        .bind("downstreamServerURL", "http://localhost:" + Integer.toString(downStreamServer.port()))
        .bind("rateLimit", 999)
        .then();

    StepVerifier.create(insertMono)
        .verifyComplete();

    downStreamServer.stubFor(get("/missingAuthorizationHeader").willReturn(aResponse().withStatus(200)));

    this.webTestClient = webTestClient.mutate()
        .responseTimeout(Duration.ofSeconds(30))
        .build();

    webTestClient.get()
        .uri("/missingAuthorizationHeader")
        .exchange()
        .expectStatus()
        .isBadRequest();

    downStreamServer.verify(exactly(0), getRequestedFor(urlEqualTo("/missingAuthorizationHeader")));
  }

  @Test
  void malformedBearerHeader() {
    var insertMono = databaseClient.sql("INSERT INTO routes VALUES(:path, :method, :downstreamServerURL, :rateLimit)")
        .bind("path", "/malformedBearerHeader")
        .bind("method", "GET")
        .bind("downstreamServerURL", "http://localhost:" + Integer.toString(downStreamServer.port()))
        .bind("rateLimit", 999)
        .then();

    StepVerifier.create(insertMono)
        .verifyComplete();

    downStreamServer.stubFor(get("/malformedBearerHeader").willReturn(aResponse().withStatus(200)));

    // No 'Bearer' in header
    this.webTestClient = webTestClient.mutate()
        .defaultHeader(HttpHeaders.AUTHORIZATION,
            mint("Springboot Test User", Duration.ofMinutes(5), trustedSigningKey, "api-gateway",
                "http://localhost:8000"))
        .responseTimeout(Duration.ofSeconds(30))
        .build();

    webTestClient.get()
        .uri("/malformedBearerHeader")
        .exchange()
        .expectStatus()
        .isBadRequest();

    downStreamServer.verify(exactly(0), getRequestedFor(urlEqualTo("/malformedBearerHeader")));
  }

  @Test
  void publicPathBypass() {
    var insertActuatorMono = databaseClient
        .sql("INSERT INTO routes VALUES(:path, :method, :downstreamServerURL, :rateLimit)")
        .bind("path", "/actuator")
        .bind("method", "GET")
        .bind("downstreamServerURL", "http://localhost:" + Integer.toString(downStreamServer.port()))
        .bind("rateLimit", 999)
        .then();

    StepVerifier.create(insertActuatorMono)
        .verifyComplete();

    var insertTestMono = databaseClient
        .sql("INSERT INTO routes VALUES(:path, :method, :downstreamServerURL, :rateLimit)")
        .bind("path", "/test")
        .bind("method", "GET")
        .bind("downstreamServerURL", "http://localhost:" + Integer.toString(downStreamServer.port()))
        .bind("rateLimit", 999)
        .then();

    StepVerifier.create(insertTestMono)
        .verifyComplete();

    // No auth header needed
    this.webTestClient = webTestClient.mutate()
        .responseTimeout(Duration.ofSeconds(30))
        .build();

    webTestClient.get()
        .uri("/actuator")
        .exchange()
        .expectStatus()
        .isOk();

    // 'Test' public path
    downStreamServer.stubFor(get("/test").willReturn(aResponse().withStatus(200)));
    webTestClient.get()
        .uri("/test")
        .exchange()
        .expectStatus()
        .isOk();

    downStreamServer.verify(exactly(1), getRequestedFor(urlEqualTo("/test")));
  }

  @Test
  void existingPathWithValidMethod() {
    var insertMono = databaseClient
        .sql("INSERT INTO routes VALUES(:path, :method, :downstreamServerURL, :rateLimit)")
        .bind("path", "/existingPathWithValidMethod")
        .bind("method", "GET")
        .bind("downstreamServerURL", "http://localhost:" + Integer.toString(downStreamServer.port()))
        .bind("rateLimit", 999)
        .then();

    StepVerifier.create(insertMono)
        .verifyComplete();

    this.webTestClient = webTestClient.mutate()
        .responseTimeout(Duration.ofSeconds(30))
        .defaultHeader(HttpHeaders.AUTHORIZATION,
            "Bearer " + mint("Springboot Test User", Duration.ofMinutes(5), trustedSigningKey, "api-gateway",
                "http://localhost:8000"))
        .build();

    downStreamServer.stubFor(get("/existingPathWithValidMethod").willReturn(aResponse().withStatus(200)));
    webTestClient.get()
        .uri("/existingPathWithValidMethod")
        .exchange()
        .expectStatus()
        .isOk();

    downStreamServer.verify(exactly(1), getRequestedFor(urlEqualTo("/existingPathWithValidMethod")));
  }

  @Test
  void unknownPath() {
    this.webTestClient = webTestClient.mutate()
        .responseTimeout(Duration.ofSeconds(30))
        .defaultHeader(HttpHeaders.AUTHORIZATION,
            "Bearer " + mint("Springboot Test User", Duration.ofMinutes(5), trustedSigningKey, "api-gateway",
                "http://localhost:8000"))
        .build();

    downStreamServer.stubFor(get("/unknownPath").willReturn(aResponse().withStatus(200)));
    webTestClient.get()
        .uri("/unknownPath")
        .exchange()
        .expectStatus()
        .isNotFound();

    downStreamServer.verify(exactly(0), getRequestedFor(urlEqualTo("/unknownPath")));
  }

  @Test
  void existingPathWithInvalidMethod() {
    var insertMono1 = databaseClient
        .sql("INSERT INTO routes VALUES(:path, :method, :downstreamServerURL, :rateLimit)")
        .bind("path", "/existingPathWithInvalidMethod")
        .bind("method", "GET")
        .bind("downstreamServerURL", "http://localhost:" + Integer.toString(downStreamServer.port()))
        .bind("rateLimit", 999)
        .then();

    StepVerifier.create(insertMono1)
        .verifyComplete();

    var insertMono2 = databaseClient
        .sql("INSERT INTO routes VALUES(:path, :method, :downstreamServerURL, :rateLimit)")
        .bind("path", "/existingPathWithInvalidMethod")
        .bind("method", "PUT")
        .bind("downstreamServerURL", "http://localhost:" + Integer.toString(downStreamServer.port()))
        .bind("rateLimit", 999)
        .then();

    StepVerifier.create(insertMono2)
        .verifyComplete();

    downStreamServer.stubFor(get("/existingPathWithInvalidMethod").willReturn(aResponse().withStatus(200)));

    this.webTestClient = webTestClient.mutate()
        .responseTimeout(Duration.ofSeconds(30))
        .defaultHeader(HttpHeaders.AUTHORIZATION,
            "Bearer " + mint("Springboot Test User", Duration.ofMinutes(5), trustedSigningKey, "api-gateway",
                "http://localhost:8000"))
        .build();

    // Should return allow header
    webTestClient.post()
        .uri("/existingPathWithInvalidMethod")
        .exchange()
        .expectStatus()
        .isEqualTo(405)
        .expectHeader()
        .valueEquals("Allow", "GET, PUT");

    downStreamServer.verify(exactly(0), postRequestedFor(urlEqualTo("/existingPathWithInvalidMethod")));
  }

  @Test
  void preservesQueryParams() {
    var insertMono = databaseClient
        .sql("INSERT INTO routes VALUES(:path, :method, :downstreamServerURL, :rateLimit)")
        .bind("path", "/preservesQueryParams")
        .bind("method", "GET")
        .bind("downstreamServerURL", "http://localhost:" + Integer.toString(downStreamServer.port()))
        .bind("rateLimit", 999)
        .then();

    StepVerifier.create(insertMono)
        .verifyComplete();

    downStreamServer.stubFor(get(urlPathEqualTo("/preservesQueryParams")).withQueryParam("name", equalTo("jesmond"))
        .withQueryParam("tag", equalTo("spring")).willReturn(aResponse().withStatus(200)));

    this.webTestClient = webTestClient.mutate()
        .responseTimeout(Duration.ofSeconds(30))
        .defaultHeader(HttpHeaders.AUTHORIZATION,
            "Bearer " + mint("Springboot Test User", Duration.ofMinutes(5), trustedSigningKey, "api-gateway",
                "http://localhost:8000"))
        .build();

    // Should return allow header
    webTestClient.get()
        .uri("/preservesQueryParams?name=jesmond&tag=spring")
        .exchange()
        .expectStatus()
        .isOk();

    downStreamServer.verify(exactly(1),
        getRequestedFor(urlPathEqualTo("/preservesQueryParams"))
            .withQueryParam("name", equalTo("jesmond"))
            .withQueryParam("tag", equalTo("spring")));
  }

  @Test
  void filterHeader() {
    var insertMono = databaseClient
        .sql("INSERT INTO routes VALUES(:path, :method, :downstreamServerURL, :rateLimit)")
        .bind("path", "/filterHeader")
        .bind("method", "GET")
        .bind("downstreamServerURL", "http://localhost:" + Integer.toString(downStreamServer.port()))
        .bind("rateLimit", 999)
        .then();

    StepVerifier.create(insertMono)
        .verifyComplete();

    downStreamServer.stubFor(get(urlPathEqualTo("/filterHeader")).willReturn(aResponse().withStatus(200)));

    this.webTestClient = webTestClient.mutate()
        .responseTimeout(Duration.ofSeconds(30))
        .defaultHeader(HttpHeaders.AUTHORIZATION,
            "Bearer " + mint("Springboot Test User", Duration.ofMinutes(5), trustedSigningKey, "api-gateway",
                "http://localhost:8000"))
        .defaultHeader(HttpHeaders.CONNECTION, "keep-alive")
        .defaultHeader("Custom-Header", "custom header")
        .build();

    // Should return allow header
    webTestClient.get()
        .uri("/filterHeader")
        .exchange()
        .expectStatus()
        .isOk();

    downStreamServer.verify(exactly(1),
        getRequestedFor(urlPathEqualTo("/filterHeader")));

    downStreamServer.verify(getRequestedFor(urlEqualTo("/filterHeader"))
        .withHeader("Host", equalTo("localhost:" + downStreamServer.port()))
        .withHeader("X-Client-Id", equalTo("Springboot Test User"))
        .withHeader("Custom-Header", equalTo("custom header"))
        .withoutHeader("Authorization")
        .withoutHeader("Connection"));
  }

  @Test
  void preservePayload() {
    var insertMono = databaseClient
        .sql("INSERT INTO routes VALUES(:path, :method, :downstreamServerURL, :rateLimit)")
        .bind("path", "/preservePayload")
        .bind("method", "POST")
        .bind("downstreamServerURL", "http://localhost:" + Integer.toString(downStreamServer.port()))
        .bind("rateLimit", 999)
        .then();

    StepVerifier.create(insertMono)
        .verifyComplete();

    downStreamServer.stubFor(post(urlPathEqualTo("/preservePayload")).willReturn(aResponse().withStatus(200)));

    this.webTestClient = webTestClient.mutate()
        .responseTimeout(Duration.ofSeconds(30))
        .defaultHeader(HttpHeaders.AUTHORIZATION,
            "Bearer " + mint("Springboot Test User", Duration.ofMinutes(5), trustedSigningKey, "api-gateway",
                "http://localhost:8000"))
        .build();

    // Should return allow header
    webTestClient.post()
        .uri("/preservePayload")
        .body(Mono.just("payload".getBytes(StandardCharsets.UTF_8)), byte[].class)
        .exchange()
        .expectStatus()
        .isOk();

    downStreamServer.verify(exactly(1),
        postRequestedFor(urlPathEqualTo("/preservePayload")));

    downStreamServer.verify(postRequestedFor(urlEqualTo("/preservePayload"))
        .withRequestBody(equalTo("payload")));

  }

  @Test
  void preserveDownstreamResponse() {
    // Response status, headers, content type and body should be retained
    var insertMono = databaseClient
        .sql("INSERT INTO routes VALUES(:path, :method, :downstreamServerURL, :rateLimit)")
        .bind("path", "/preserveDownstreamResponse")
        .bind("method", "GET")
        .bind("downstreamServerURL", "http://localhost:" + Integer.toString(downStreamServer.port()))
        .bind("rateLimit", 999)
        .then();

    StepVerifier.create(insertMono)
        .verifyComplete();

    downStreamServer.stubFor(get(urlPathEqualTo("/preserveDownstreamResponse")).willReturn(
        aResponse()
            .withHeader("Custom-Header", "custom header")
            .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .withBody("{\"payload\": \"payload\"}")
            .withStatus(200)));

    this.webTestClient = webTestClient.mutate()
        .responseTimeout(Duration.ofSeconds(30))
        .defaultHeader(HttpHeaders.AUTHORIZATION,
            "Bearer " + mint("Springboot Test User", Duration.ofMinutes(5), trustedSigningKey, "api-gateway",
                "http://localhost:8000"))
        .build();

    // Should return allow header
    webTestClient.get()
        .uri("/preserveDownstreamResponse")
        .exchange()
        .expectStatus()
        .isOk()
        .expectHeader()
        .valueEquals("Custom-Header", "custom header")
        .expectHeader()
        .valueEquals("Content-Type", "application/json")
        .expectBody(byte[].class)
        .isEqualTo("payload".getBytes(StandardCharsets.UTF_8));

    downStreamServer.verify(exactly(1),
        getRequestedFor(urlPathEqualTo("/preserveDownstreamResponse")));
  }
}
