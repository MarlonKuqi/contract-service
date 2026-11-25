package com.mk.contractservice.features.shared;

import org.springframework.transaction.annotation.Transactional;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a use case handler that writes: it runs in a read-write transaction.
 * <p>A handler that only reads uses {@link QueryUseCase} instead. Choosing between the two annotations is the
 * transaction decision of the use case: a handler cannot run without a transaction.
 * <p>Handlers orchestrate: they load aggregates through the ports, delegate the rules to the domain and call the
 * ports. They hold no business rule. Bean registration is done by {@code DomainBeanConfiguration}.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Transactional
public @interface UseCase {
}
