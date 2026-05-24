package com.mk.contractservice.infrastructure.config;

import com.mk.contractservice.domain.shared.DomainService;
import com.mk.contractservice.domain.shared.UseCase;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;

/**
 * Infrastructure configuration responsible for registering domain beans with Spring.
 *
 * <h2>Why this class exists</h2>
 * <p>This architecture mixes Vertical Slices (features/) and Clean Architecture
 * (dependency rule: domain has no framework dependency). As a consequence,
 * annotations such as {@link DomainService} and {@link UseCase} are intentionally
 * pure Java markers — they carry no Spring semantics.
 *
 * <p>This class is the single place where the infrastructure layer decides how
 * domain and feature classes become Spring-managed beans. The domain stays
 * framework-agnostic; the infrastructure owns the wiring.
 *
 * <h2>Scanned annotations</h2>
 * <ul>
 *   <li>{@link DomainService} — domain services in {@code domain.*}</li>
 *   <li>{@link UseCase} — use case handlers in {@code features.*}</li>
 * </ul>
 *
 * <p>{@code useDefaultFilters = false} ensures that only classes explicitly
 * annotated with the above markers are registered by this scan, avoiding
 * double-registration with the main {@code @SpringBootApplication} component scan.
 */
@Configuration
@ComponentScan(
        basePackages = {
                "com.mk.contractservice.domain",
                "com.mk.contractservice.features"
        },
        useDefaultFilters = false,
        includeFilters = {
                @ComponentScan.Filter(type = FilterType.ANNOTATION, classes = DomainService.class),
                @ComponentScan.Filter(type = FilterType.ANNOTATION, classes = UseCase.class)
        }
)
public class DomainBeanConfiguration {
}

