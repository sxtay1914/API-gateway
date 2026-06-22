package com.jesmond.api_gateway;

import java.util.List;

import org.springframework.stereotype.Component;
import org.springframework.web.server.WebFilter;

import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.jwk.JWK;

@Component
public class JwtFilter implements WebFilter {

  private List<JWK> keySets;

  public JwtFilter() {
    this.keySets = keySets;

  }
}
