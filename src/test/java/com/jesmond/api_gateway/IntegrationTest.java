package com.jesmond.api_gateway;

import org.junit.jupiter.api.Test;
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
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import lombok.var;
import reactor.test.StepVerifier;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

import org.springframework.http.HttpHeaders;
import org.springframework.r2dbc.core.DatabaseClient;

import java.time.Duration;
import java.time.Instant;
import java.util.Date;

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
  static final RSAKey signingKey;
  @Autowired
  private DatabaseClient databaseClient;
  @Autowired
  private WebTestClient webTestClient;
  @Autowired
  private ReactiveRedisTemplate<String, String> redisTemplate;

  private String mint(String sub, Duration ttl) {
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
      // create signingKey
      signingKey = new RSAKeyGenerator(2048).keyID("my-key-id").algorithm(JWSAlgorithm.RS256).keyUse(KeyUse.SIGNATURE)
          .generate();
    } catch (JOSEException e) {
      throw new AssertionError("Failed to generate signing key" + e);
    }
    // create stub point for well-known/jwks.json
    authMockServer
        .stubFor(get("/.well-known/jwks.json").willReturn(okJson(new JWKSet(signingKey.toPublicJWK()).toString())));

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

    this.webTestClient = webTestClient.mutate()
        .responseTimeout(Duration.ofSeconds(30))
        .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + mint("Springboot Test User", Duration.ofMinutes(5)))
        .build();
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
  void validJwtHappyPath() {
    // Insert downstreamServer record into sql
    var insertMono = databaseClient.sql("INSERT INTO routes VALUES(:path, :method, :downstreamServerURL, :rateLimit)")
        .bind("path", "/jwtHappy")
        .bind("method", "GET")
        .bind("downstreamServerURL", "http://localhost:" + Integer.toString(downStreamServer.port()))
        .bind("rateLimit", 999)
        .then();

    StepVerifier.create(insertMono)
        .verifyComplete();

    // create stub for downstream server
    downStreamServer.stubFor(get("/jwtHappy").willReturn(aResponse().withStatus(200)));

    // Hit api gateway endpoint
    // Assert 200 ok response received
    webTestClient.get()
        .uri("/jwtHappy")
        .exchange()
        .expectStatus()
        .isOk();

    downStreamServer
        .verify(exactly(1), getRequestedFor(urlEqualTo("/jwtHappy")));
  }
}
