package com.mercadona.prueba.snk.digitaldocument.driven.repositories.metrics;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import javax.sql.DataSource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class DatabaseTableMetricsCustomTest {

  private static final String MICROSERVICE = "snk-digital-document";
  private static final String TABLE_NAME   = "topic_consumer_error";
  private static final String METRIC_NAME  = "db.table.rows";

  // ---------------------------------------------------------------------------
  // bindTo — happy path: Gauge registrado con tags correctos
  // ---------------------------------------------------------------------------

  @Test
  @DisplayName("Should register Gauge with correct metric name and tags after construction")
  void should_register_gauge_with_correct_name_and_tags() {
    MeterRegistry registry = new SimpleMeterRegistry();
    DataSource dataSource = mock(DataSource.class);

    new DatabaseTableMetricsCustom(registry, dataSource, MICROSERVICE, TABLE_NAME, METRIC_NAME);

    Gauge gauge = registry.find(METRIC_NAME).gauge();
    assertThat(gauge).isNotNull();
    assertThat(gauge.getId().getTag("micro")).isEqualTo(MICROSERVICE);
    assertThat(gauge.getId().getTag("table")).isEqualTo(TABLE_NAME);
  }

  // ---------------------------------------------------------------------------
  // bindTo — happy path: Gauge devuelve el count real de la DB
  // ---------------------------------------------------------------------------

  @Test
  @DisplayName("Should return row count from database when SQL executes successfully")
  void should_return_row_count_when_sql_executes_successfully() throws Exception {
    MeterRegistry registry = new SimpleMeterRegistry();
    DataSource dataSource = mock(DataSource.class);
    Connection conn = mock(Connection.class);
    PreparedStatement ps = mock(PreparedStatement.class);
    ResultSet rs = mock(ResultSet.class);

    when(dataSource.getConnection()).thenReturn(conn);
    when(conn.prepareStatement(anyString())).thenReturn(ps);
    when(ps.executeQuery()).thenReturn(rs);
    when(rs.next()).thenReturn(true);
    when(rs.getInt(1)).thenReturn(7);

    new DatabaseTableMetricsCustom(registry, dataSource, MICROSERVICE, TABLE_NAME, METRIC_NAME);

    Gauge gauge = registry.find(METRIC_NAME).gauge();
    assertThat(gauge).isNotNull();
    assertThat(gauge.value()).isEqualTo(7.0);
  }

  // ---------------------------------------------------------------------------
  // bindTo — error path: SQLException → Gauge devuelve 0.0 (no falla)
  // ---------------------------------------------------------------------------

  @Test
  @DisplayName("Should return 0.0 from Gauge when DataSource throws SQLException")
  void should_return_zero_when_data_source_throws_sql_exception() throws Exception {
    MeterRegistry registry = new SimpleMeterRegistry();
    DataSource dataSource = mock(DataSource.class);

    when(dataSource.getConnection()).thenThrow(new SQLException("connection refused"));

    new DatabaseTableMetricsCustom(registry, dataSource, MICROSERVICE, TABLE_NAME, METRIC_NAME);

    Gauge gauge = registry.find(METRIC_NAME).gauge();
    assertThat(gauge).isNotNull();
    assertThat(gauge.value()).isEqualTo(0.0);
  }

  // ---------------------------------------------------------------------------
  // bindTo — descripcion y unidad correctas
  // ---------------------------------------------------------------------------

  @Test
  @DisplayName("Should register Gauge with correct description and base unit")
  void should_register_gauge_with_correct_description_and_unit() {
    MeterRegistry registry = new SimpleMeterRegistry();
    DataSource dataSource = mock(DataSource.class);

    new DatabaseTableMetricsCustom(registry, dataSource, MICROSERVICE, TABLE_NAME, METRIC_NAME);

    Gauge gauge = registry.find(METRIC_NAME).gauge();
    assertThat(gauge).isNotNull();
    assertThat(gauge.getId().getDescription()).contains(TABLE_NAME);
    assertThat(gauge.getId().getBaseUnit()).isEqualTo("rows");
  }
}
