package com.mercadona.prueba.snk.digitaldocument.driven.repositories.metrics;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.function.ToDoubleFunction;

public class DatabaseTableMetricsCustom implements MeterBinder {

  private static final String QUERY = "SELECT COUNT(1) FROM ";

  private final DataSource dataSource;
  private final String microService;
  private final String tableName;
  private final String metricName;

  public DatabaseTableMetricsCustom(MeterRegistry registry, DataSource dataSource,
                                    String microService, String tableName, String metricName) {
    this.dataSource = dataSource;
    this.microService = microService;
    this.tableName = tableName;
    this.metricName = metricName;
    bindTo(registry);
  }

  @Override
  public void bindTo(MeterRegistry registry) {
    ToDoubleFunction<DataSource> countRows = ds -> {
      try (var conn = ds.getConnection();
           var ps = conn.prepareStatement(QUERY + this.tableName);
           var rs = ps.executeQuery()) {
        rs.next();
        return rs.getInt(1);
      } catch (SQLException e) {
        return 0.0;
      }
    };

    Gauge.builder(this.metricName, this.dataSource, countRows)
        .tag("micro", this.microService)
        .tag("table", this.tableName)
        .description("Number of rows in database table " + this.tableName)
        .baseUnit("rows")
        .register(registry);
  }
}
