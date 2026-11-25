package com.mk.contractservice.controllers.shared;

import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.ValueDeserializer;

import java.util.Optional;

public class OptionalNonNullStringDeserializer extends ValueDeserializer<Optional<String>> {

    @Override
    public Optional<String> deserialize(final JsonParser parser, final DeserializationContext context) {
        return Optional.of(context.readValue(parser, String.class));
    }

    @Override
    public Optional<String> getNullValue(final DeserializationContext context) {
        return context.reportInputMismatch(this, "A field that can only be omitted must not be null");
    }

    @Override
    public Object getAbsentValue(final DeserializationContext context) {
        return Optional.empty();
    }
}
