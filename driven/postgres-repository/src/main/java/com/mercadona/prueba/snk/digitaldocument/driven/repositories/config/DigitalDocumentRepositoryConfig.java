package com.mercadona.prueba.snk.digitaldocument.driven.repositories.config;

import com.mercadona.prueba.snk.digitaldocument.driven.repositories.DigitalDocumentJpaRepository;
import com.mercadona.prueba.snk.digitaldocument.driven.repositories.models.DigitalDocumentMO;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@EnableJpaRepositories(basePackageClasses = DigitalDocumentJpaRepository.class)
@EntityScan(basePackageClasses = DigitalDocumentMO.class)
public class DigitalDocumentRepositoryConfig {
}
