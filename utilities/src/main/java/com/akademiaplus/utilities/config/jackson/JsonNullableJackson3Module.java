/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.utilities.config.jackson;

import org.openapitools.jackson.nullable.JsonNullable;
import tools.jackson.core.Version;
import tools.jackson.databind.JacksonModule;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.ValueSerializer;
import tools.jackson.databind.deser.Deserializers;
import tools.jackson.databind.ser.Serializers;
import tools.jackson.databind.type.ReferenceType;

/**
 * Jackson 3 module that registers serialization and deserialization support
 * for {@link JsonNullable}, the OpenAPI nullable wrapper type.
 * <p>
 * This module is the Jackson 3 equivalent of the Jackson 2
 * {@code org.openapitools.jackson.nullable.JsonNullableModule}. It registers:
 * <ul>
 *   <li>A {@link JsonNullableJackson3TypeModifier} so Jackson treats
 *       {@code JsonNullable<T>} as a reference type</li>
 *   <li>A {@link JsonNullableJackson3Serializer} for serialization</li>
 *   <li>A {@link JsonNullableJackson3Deserializer} for deserialization</li>
 * </ul>
 *
 * @author ElatusDev
 * @since 1.0
 */
public class JsonNullableJackson3Module extends JacksonModule {

    /** Module name used for registration identity. */
    public static final String MODULE_NAME = "JsonNullableJackson3Module";

    @Override
    public String getModuleName() {
        return MODULE_NAME;
    }

    @Override
    public Version version() {
        return Version.unknownVersion();
    }

    @Override
    public Object getRegistrationId() {
        return MODULE_NAME;
    }

    @Override
    public void setupModule(SetupContext context) {
        context.addTypeModifier(new JsonNullableJackson3TypeModifier());
        context.addSerializers(new JsonNullableSerializersProvider());
        context.addDeserializers(new JsonNullableDeserializersProvider());
    }

    /**
     * Serializer provider that creates {@link JsonNullableJackson3Serializer}
     * instances for {@link JsonNullable} reference types.
     */
    private static class JsonNullableSerializersProvider extends Serializers.Base {
        @Override
        public ValueSerializer<?> findReferenceSerializer(
                tools.jackson.databind.SerializationConfig config,
                ReferenceType refType,
                tools.jackson.databind.BeanDescription.Supplier beanDescSupplier,
                com.fasterxml.jackson.annotation.JsonFormat.Value formatOverrides,
                tools.jackson.databind.jsontype.TypeSerializer contentTypeSerializer,
                ValueSerializer<Object> contentValueSerializer) {
            if (refType.hasRawClass(JsonNullable.class)) {
                return new JsonNullableJackson3Serializer(
                        refType, false, contentTypeSerializer, contentValueSerializer);
            }
            return null;
        }
    }

    /**
     * Deserializer provider that creates {@link JsonNullableJackson3Deserializer}
     * instances for {@link JsonNullable} reference types.
     */
    private static class JsonNullableDeserializersProvider extends Deserializers.Base {
        @Override
        public ValueDeserializer<?> findReferenceDeserializer(
                ReferenceType refType,
                tools.jackson.databind.DeserializationConfig config,
                tools.jackson.databind.BeanDescription.Supplier beanDescSupplier,
                tools.jackson.databind.jsontype.TypeDeserializer contentTypeDeserializer,
                ValueDeserializer<?> contentDeserializer) {
            if (refType.hasRawClass(JsonNullable.class)) {
                return new JsonNullableJackson3Deserializer(
                        refType, null, contentTypeDeserializer, contentDeserializer);
            }
            return null;
        }

        @Override
        public boolean hasDeserializerFor(
                tools.jackson.databind.DeserializationConfig config,
                Class<?> valueType) {
            return JsonNullable.class.isAssignableFrom(valueType);
        }
    }
}
