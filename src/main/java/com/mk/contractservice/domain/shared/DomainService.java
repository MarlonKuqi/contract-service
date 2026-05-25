package com.mk.contractservice.domain.shared;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a class as a Domain Service in the sense of Domain-Driven Design.
 *
 * <h2>Role</h2>
 * <p>A Domain Service encapsulates domain logic that does not naturally belong
 * to a single Entity or Value Object. It operates exclusively on domain concepts
 * and must have no dependency on infrastructure concerns (JPA, HTTP, cache, etc.).
 *
 * <h2>Architecture</h2>
 * <p>This project follows a mix of <strong>Vertical Slices</strong> and
 * <strong>Clean Architecture</strong> with hexagonal influences:
 * <ul>
 *   <li>{@code domain/} — pure domain model (entities, value objects, ports, domain services)</li>
 *   <li>{@code features/} — vertical slices, one per use case (see {@link UseCase})</li>
 *   <li>{@code infrastructure/} — adapters: JPA, cache, HTTP controllers</li>
 * </ul>
 * The dependency rule flows inward: {@code infrastructure → features → domain}.
 * The domain must not depend on any outer layer.
 *
 * <h2>Transaction boundary</h2>
 * <p>Domain services do NOT declare {@code @Transactional}. Transaction boundaries
 * are exclusively defined at the {@link UseCase} handler level. A domain service
 * always runs inside an already-active transaction (Spring's default
 * {@code PROPAGATION_REQUIRED} ensures this). This keeps the domain free of
 * framework-specific transaction semantics.
 *
 * <h2>Spring wiring</h2>
 * <p>This annotation is a pure domain marker — it carries no Spring semantics.
 * Spring bean registration for {@code @DomainService}-annotated classes is
 * delegated to {@code DomainBeanConfiguration} in the infrastructure layer.
 * The domain knows nothing about how it is instantiated.
 *
 * @see UseCase
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DomainService {
}

