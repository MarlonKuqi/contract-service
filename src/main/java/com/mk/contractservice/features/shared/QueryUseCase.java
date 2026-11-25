package com.mk.contractservice.features.shared;

import org.springframework.transaction.annotation.Transactional;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a use case handler that only reads: it runs in a read-only transaction.
 *
 * @see UseCase
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Transactional(readOnly = true)
public @interface QueryUseCase {
}
