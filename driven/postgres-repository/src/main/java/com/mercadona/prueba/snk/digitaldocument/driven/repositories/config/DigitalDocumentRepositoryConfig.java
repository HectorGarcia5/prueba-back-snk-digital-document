package com.mercadona.prueba.snk.digitaldocument.driven.repositories.config;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@EnableJpaRepositories(basePackages = "com.mercadona.prueba.snk.digitaldocument.driven.repositories")
@EntityScan(basePackages = "com.mercadona.prueba.snk.digitaldocument.driven.repositories.models")
@ComponentScan({
    "com.mercadona.framework.cna.commons.outbox.jpa.configuration",
    "com.mercadona.prueba.snk.digitaldocument.driven.repositories"
})
public class DigitalDocumentRepositoryConfig {
}
