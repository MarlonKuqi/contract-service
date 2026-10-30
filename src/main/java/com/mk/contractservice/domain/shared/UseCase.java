package com.mk.contractservice.domain.shared;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a class as a Use Case handler in the application layer ({@code features/}).
 *
 * <h2>Role</h2>
 * <p>A Use Case handler is a vertical slice orchestrator: it receives an intent
 * (Command or Query), enforces pre-conditions (via domain services), delegates
 * to domain objects or repository ports, and defines the transaction boundary.
 *
 * <h2>Architecture</h2>
 * <p>This project follows a mix of <strong>Vertical Slices</strong> and
 * <strong>Clean Architecture</strong> with hexagonal influences.
 * Each use case lives in {@code features/} as a self-contained interface + Handler:
 * <pre>
 * public interface CreateContract {
 *     record Command(...) {}
 *     Contract execute(Command command);
 *
 *     {@literal @}UseCase
 *     {@literal @}Transactional
 *     class Handler implements CreateContract { ... }
 * }
 * </pre>
 *
 * <h2>Rules</h2>
 * <ul>
 *   <li>Must NOT contain domain logic (no business rules, no invariants)</li>
 *   <li>Must NOT depend on infrastructure directly (no JPA, no HTTP)</li>
 *   <li>MUST declare {@code @Transactional} or {@code @Transactional(readOnly = true)}
 *       explicitly — the transaction boundary is a use-case-level decision and must
 *       be visible at the definition site</li>
 * </ul>
 *
 * <h2>Transaction boundary</h2>
 * <p>{@code @Transactional} is intentionally NOT embedded in this annotation.
 * The choice between a read-only and a write transaction is meaningful and must
 * remain explicit per handler. Embedding it here would hide that decision.
 *
 * <h2>Spring wiring</h2>
 * <p>This annotation is a pure marker — it carries no Spring semantics.
 * Spring bean registration is handled by {@code DomainBeanConfiguration}
 * in the infrastructure layer.
 *
 * @see DomainService
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface UseCase {
}


