package com.jesmond.api_gateway;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import java.net.URI;

@Configuration
public class JwtConfig {

  // JWKS = JSON Web Key Set
  @Value("${auth_server_url}")
  private String jwksURL;

  @Bean
  public List<JWK> jwkList() {
    /*
     * Get publc key from JWKS endpoint
     * return key sets
     */
    try {
      JWKSet jwkSet = JWKSet.load(new URI(jwksURL).toURL());
      List<JWK> keySets = jwkSet.getKeys();
      return keySets;

    } catch (Exception e) {
      throw new RuntimeException(e);
    }

  }
}
