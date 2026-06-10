package com.mk.contractservice.domain.shared;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks an interface as a Repository Port in the domain layer.
 *
 * <h2>What is a Repository Port?</h2>
 * <p>A repository port is a secondary port (driven side) that expresses
 * the domain's need to persist or retrieve data, without knowing how this is achieved.
 * The infrastructure layer provides an adapter (e.g., a JPA implementation) that
 * fulfills this contract.
 *
 * <h2>Why not {@link DomainService} or {@link UseCase}?</h2>
 * <p>Unlike {@link DomainService} and {@link UseCase}, this annotation has
 * <strong>no Spring wiring effect whatsoever</strong>. Repository ports are Java
 * interfaces — they are never Spring beans. Only their infrastructure implementations
 * carry Spring's {@code @Repository}.
 * <p>This annotation is <strong>purely documentary</strong>. It signals the
 * architectural role of the interface and can be leveraged in ArchUnit rules
 * (e.g., "all implementations of {@code @RepositoryPort} interfaces must reside
 * in {@code infrastructure.*}").
 *
 * <h2>Scope — aggregate repositories vs. query ports</h2>
 * <p>This annotation applies to two kinds of domain interfaces:
 * <ul>
 *   <li><strong>Aggregate repositories</strong> — manage the full lifecycle of a
 *       single aggregate root ({@code save}, {@code findById}, {@code delete}).
 *       In DDD, a repository is conceptually a collection of aggregates.</li>
 *   <li><strong>Query ports</strong> — expose analytical projections that aggregate
 *       values across multiple domain objects (e.g., a {@code SUM} across contracts).
 *       These methods do not reconstitute any aggregate and therefore do not belong
 *       in a repository. Extracting them is an application of the
 *       <em>Interface Segregation Principle</em> (ISP) in a DDD context —
 *       not CQRS, which would require a fully separate read model and write model.</li>
 * </ul>
 *
 * @see DomainService
 * @see UseCase
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RepositoryPort {
}
