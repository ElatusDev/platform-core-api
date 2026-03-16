/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.utilities.config.jackson;

import org.openapitools.jackson.nullable.JsonNullable;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.deser.ValueInstantiator;
import tools.jackson.databind.deser.std.ReferenceTypeDeserializer;
import tools.jackson.databind.jsontype.TypeDeserializer;

/**
 * Jackson 3 deserializer for {@link JsonNullable}.
 * <p>
 * Wraps deserialized values in {@link JsonNullable#of(Object)} and returns
 * {@link JsonNullable#of(Object)} with {@code null} for JSON {@code null},
 * and {@link JsonNullable#undefined()} when the field is absent from the
 * JSON payload.
 *
 * @author ElatusDev
 * @since 1.0
 */
public class JsonNullableJackson3Deserializer
        extends ReferenceTypeDeserializer<JsonNullable<?>> {

    /**
     * Creates the deserializer.
     *
     * @param fullType            the full generic type being deserialized
     * @param valueInstantiator   value instantiator (unused for reference types)
     * @param typeDeserializer    optional polymorphic type deserializer
     * @param valueDeserializer   deserializer for the contained value type
     */
    public JsonNullableJackson3Deserializer(
            JavaType fullType,
            ValueInstantiator valueInstantiator,
            TypeDeserializer typeDeserializer,
            ValueDeserializer<?> valueDeserializer) {
        super(fullType, valueInstantiator, typeDeserializer, valueDeserializer);
    }

    @Override
    public JsonNullableJackson3Deserializer withResolved(
            TypeDeserializer typeDeser,
            ValueDeserializer<?> valueDeser) {
        return new JsonNullableJackson3Deserializer(
                _fullType, _valueInstantiator, typeDeser, valueDeser);
    }

    @Override
    public JsonNullable<?> getNullValue(DeserializationContext ctxt) {
        return JsonNullable.of(null);
    }

    @Override
    public Object getAbsentValue(DeserializationContext ctxt) {
        return JsonNullable.undefined();
    }

    @Override
    public JsonNullable<?> referenceValue(Object contents) {
        return JsonNullable.of(contents);
    }

    @Override
    public Object getReferenced(JsonNullable<?> reference) {
        return reference.get();
    }

    @Override
    public JsonNullable<?> updateReference(
            JsonNullable<?> reference, Object contents) {
        return JsonNullable.of(contents);
    }
}
