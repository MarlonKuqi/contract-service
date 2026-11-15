package com.mk.contractservice.controllers.client.shared;

import com.mk.contractservice.domain.shared.Residence;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.ValueDeserializer;

import java.util.Optional;

public class OptionalNonNullResidenceDeserializer extends ValueDeserializer<Optional<Residence>> {

    @Override
    public Optional<Residence> deserialize(final JsonParser parser, final DeserializationContext context) {
        return Optional.of(context.readValue(parser, Residence.class));
    }

    @Override
    public Optional<Residence> getNullValue(final DeserializationContext context) {
        return context.reportInputMismatch(this, "A field that can only be omitted must not be null");
    }

    @Override
    public Object getAbsentValue(final DeserializationContext context) {
        return Optional.empty();
    }
}
