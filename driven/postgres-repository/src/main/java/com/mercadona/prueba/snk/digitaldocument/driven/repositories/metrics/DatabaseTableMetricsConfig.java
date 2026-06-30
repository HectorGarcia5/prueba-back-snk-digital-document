package com.mercadona.prueba.snk.digitaldocument.driven.repositories.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

@Slf4j
@Configuration
public class DatabaseTableMetricsConfig {

  @Value("${metric.error.table-name}")
  private String errorTableName;

  @Value("${metric.error.microservice-name}")
  private String errorMicroService;

  @Value("${metric.error.metric-name}")
  private String errorMetricName;

  private final DataSource dataSource;
  private final MeterRegistry meterRegistry;

  public DatabaseTableMetricsConfig(DataSource dataSource, MeterRegistry meterRegistry) {
    log.info("Initializing Micrometer gauge for topic_consumer_error table");
    this.dataSource = dataSource;
    this.meterRegistry = meterRegistry;
  }

  @Bean
  public DatabaseTableMetricsCustom errorDatabaseTableMetricsCustom() {
    return new DatabaseTableMetricsCustom(
        meterRegistry, dataSource, errorMicroService, errorTableName, errorMetricName);
  }
}
