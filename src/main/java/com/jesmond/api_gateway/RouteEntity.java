package com.jesmond.api_gateway;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Table("routes")
public class RouteEntity {
  @Id
  private RouteId id;
  private String dest;

  @Column("rate_limit")
  private int limit;
}
