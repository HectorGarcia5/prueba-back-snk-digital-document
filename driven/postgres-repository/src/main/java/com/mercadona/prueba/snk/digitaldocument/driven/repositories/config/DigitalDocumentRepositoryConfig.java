package com.mercadona.prueba.snk.digitaldocument.driven.repositories.config;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@EnableJpaRepositories(basePackages = "com.mercadona.prueba.snk.digitaldocument.driven.repositories")
@EntityScan(basePackages = "com.mercadona.prueba.snk.digitaldocument.driven.repositories.models")
public class DigitalDocumentRepositoryConfig {
}
