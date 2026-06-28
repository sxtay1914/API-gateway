package com.jesmond.api_gateway;

import java.net.URL;
import java.util.Collections;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.jwk.source.RemoteJWKSet;
import com.nimbusds.jose.proc.JWSKeySelector;
import com.nimbusds.jose.proc.JWSVerificationKeySelector;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.proc.ConfigurableJWTProcessor;
import com.nimbusds.jwt.proc.DefaultJWTProcessor;

import jakarta.annotation.PostConstruct;

@Service
public class JwtService {

  @Value("${auth_server_url}")
  private String jwksURLString;
  private JWKSource<SecurityContext> keySource;

  @PostConstruct
  public void init() {
    try {
      URL jwksURL = new java.net.URI(jwksURLString).toURL();
      this.keySource = new RemoteJWKSet<>(jwksURL);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public void verifyToken(String token) {
    try {
      // Set up processor
      ConfigurableJWTProcessor<SecurityContext> jwtProcessor = new DefaultJWTProcessor<>();

      // Expected algorithm
      JWSAlgorithm expectedAlg = JWSAlgorithm.RS256;

      // Bind specific algo to key selector
      JWSKeySelector<SecurityContext> keySelector = new JWSVerificationKeySelector<>(
          Collections.singleton(expectedAlg),
          keySource);

      jwtProcessor.setJWSKeySelector(keySelector);

      jwtProcessor.process(token, null);

    } catch (Exception e) {
      System.out.println(e.getMessage());
      throw new RuntimeException(e);
    }

  }
}
