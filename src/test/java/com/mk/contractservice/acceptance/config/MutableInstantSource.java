package com.mk.contractservice.acceptance.config;

import java.time.Duration;
import java.time.Instant;
import java.time.InstantSource;
import java.util.concurrent.atomic.AtomicReference;

public final class MutableInstantSource implements InstantSource {

    private final AtomicReference<Instant> instant;

    public MutableInstantSource(final Instant initialInstant) {
        this.instant = new AtomicReference<>(initialInstant);
    }

    public void setInstant(final Instant newInstant) {
        instant.set(newInstant);
    }

    public void advance(final Duration duration) {
        instant.updateAndGet(current -> current.plus(duration));
    }

    @Override
    public Instant instant() {
        return instant.get();
    }
}
