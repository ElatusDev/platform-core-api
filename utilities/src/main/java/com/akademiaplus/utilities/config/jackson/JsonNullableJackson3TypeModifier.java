/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.utilities.config.jackson;

import org.openapitools.jackson.nullable.JsonNullable;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.type.ReferenceType;
import tools.jackson.databind.type.TypeBindings;
import tools.jackson.databind.type.TypeFactory;
import tools.jackson.databind.type.TypeModifier;

import java.lang.reflect.Type;

/**
 * Jackson 3 type modifier that upgrades {@link JsonNullable} from a simple type
 * to a {@link ReferenceType}.
 * <p>
 * This is necessary so Jackson recognizes {@code JsonNullable<T>} as a reference
 * wrapper and routes it through the reference serializer/deserializer pipeline.
 *
 * @author ElatusDev
 * @since 1.0
 */
public class JsonNullableJackson3TypeModifier extends TypeModifier {

    @Override
    public JavaType modifyType(
            JavaType type,
            Type jdkType,
            TypeBindings bindings,
            TypeFactory typeFactory) {
        if (type.isReferenceType() || type.isContainerType()) {
            return type;
        }
        if (type.getRawClass() == JsonNullable.class) {
            JavaType contentType = type.containedTypeOrUnknown(0);
            return ReferenceType.upgradeFrom(type, contentType);
        }
        return type;
    }
}
