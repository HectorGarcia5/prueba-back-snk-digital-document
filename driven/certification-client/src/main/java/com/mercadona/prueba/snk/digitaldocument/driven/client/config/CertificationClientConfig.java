package com.mercadona.prueba.snk.digitaldocument.driven.client.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@Configuration
public class CertificationClientConfig {

  @Value("${clients.certification.url}")
  private String baseUrl;

  @Value("${clients.certification.connect-timeout-ms:3000}")
  private int connectTimeoutMs;

  @Value("${clients.certification.read-timeout-ms:5000}")
  private int readTimeoutMs;

  @Bean
  public RestClient certificationRestClient() {
    var factory = new SimpleClientHttpRequestFactory();
    factory.setConnectTimeout(Duration.ofMillis(connectTimeoutMs));
    factory.setReadTimeout(Duration.ofMillis(readTimeoutMs));

    return RestClient.builder()
        .baseUrl(baseUrl)
        .requestFactory(factory)
        .build();
  }
}
