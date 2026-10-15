package com.mk.contractservice.acceptance.config;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.concurrent.atomic.AtomicReference;

public final class MutableClock extends Clock {

    private final AtomicReference<Instant> instant;
    private final ZoneId zone;

    public MutableClock(final Instant initialInstant, final ZoneId zone) {
        this(new AtomicReference<>(initialInstant), zone);
    }

    private MutableClock(final AtomicReference<Instant> instant, final ZoneId zone) {
        this.instant = instant;
        this.zone = zone;
    }

    public void setInstant(final Instant newInstant) {
        instant.set(newInstant);
    }

    public void advance(final Duration duration) {
        instant.updateAndGet(current -> current.plus(duration));
    }

    @Override
    public ZoneId getZone() {
        return zone;
    }

    @Override
    public Clock withZone(final ZoneId newZone) {
        return new MutableClock(instant, newZone);
    }

    @Override
    public Instant instant() {
        return instant.get();
    }
}
