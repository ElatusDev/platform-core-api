/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.utilities.config.jackson;

import org.openapitools.jackson.nullable.JsonNullable;
import tools.jackson.databind.BeanProperty;
import tools.jackson.databind.ValueSerializer;
import tools.jackson.databind.jsontype.TypeSerializer;
import tools.jackson.databind.ser.std.ReferenceTypeSerializer;
import tools.jackson.databind.type.ReferenceType;
import tools.jackson.databind.util.NameTransformer;

/**
 * Jackson 3 serializer for {@link JsonNullable}.
 * <p>
 * Delegates to {@link ReferenceTypeSerializer} to handle the three states:
 * <ul>
 *   <li><em>undefined</em> — field is omitted from JSON output</li>
 *   <li><em>null</em> — field is present with value {@code null}</li>
 *   <li><em>present</em> — field is present with the wrapped value</li>
 * </ul>
 *
 * @author ElatusDev
 * @since 1.0
 */
public class JsonNullableJackson3Serializer
        extends ReferenceTypeSerializer<JsonNullable<?>> {

    /**
     * Primary constructor used by the module when resolving serializers.
     *
     * @param fullType        the resolved reference type
     * @param staticTyping    whether static typing is required
     * @param typeSerializer  optional type serializer for polymorphic types
     * @param serializer      content value serializer
     */
    public JsonNullableJackson3Serializer(
            ReferenceType fullType,
            boolean staticTyping,
            TypeSerializer typeSerializer,
            ValueSerializer<Object> serializer) {
        super(fullType, staticTyping, typeSerializer, serializer);
    }

    /**
     * Copy constructor used by {@link #withResolved} and {@link #withContentInclusion}.
     */
    protected JsonNullableJackson3Serializer(
            JsonNullableJackson3Serializer base,
            BeanProperty property,
            TypeSerializer typeSerializer,
            ValueSerializer<?> valueSer,
            NameTransformer unwrapper,
            Object suppressableValue,
            boolean suppressNulls) {
        super(base, property, typeSerializer, valueSer, unwrapper,
                suppressableValue, suppressNulls);
    }

    @Override
    protected ReferenceTypeSerializer<JsonNullable<?>> withResolved(
            BeanProperty prop,
            TypeSerializer typeSer,
            ValueSerializer<?> valueSer,
            NameTransformer unwrapper) {
        return new JsonNullableJackson3Serializer(
                this, prop, typeSer, valueSer, unwrapper,
                _suppressableValue, _suppressNulls);
    }

    @Override
    public ReferenceTypeSerializer<JsonNullable<?>> withContentInclusion(
            Object suppressableValue,
            boolean suppressNulls) {
        return new JsonNullableJackson3Serializer(
                this, _property, _valueTypeSerializer, _valueSerializer,
                _unwrapper, suppressableValue, suppressNulls);
    }

    @Override
    protected boolean _isValuePresent(JsonNullable<?> value) {
        return value.isPresent();
    }

    @Override
    protected Object _getReferenced(JsonNullable<?> value) {
        return value.get();
    }

    @Override
    protected Object _getReferencedIfPresent(JsonNullable<?> value) {
        return value.isPresent() ? value.get() : null;
    }
}
