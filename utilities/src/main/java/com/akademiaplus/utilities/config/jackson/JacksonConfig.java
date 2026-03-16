/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.utilities.config.jackson;

import org.openapitools.jackson.nullable.JsonNullable;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.JacksonModule;

/**
 * Jackson 3 configuration for the platform.
 * <p>
 * Registers the {@link JsonNullableJackson3Module} so that
 * {@link JsonNullable} fields in OpenAPI-generated DTOs can be properly
 * serialized and deserialized by Jackson 3's {@code ObjectMapper}.
 *
 * @author ElatusDev
 * @since 1.0
 */
@Configuration
public class JacksonConfig {

    /**
     * Creates the Jackson 3 module that handles {@link JsonNullable}
     * serialization and deserialization.
     *
     * @return the configured module
     */
    @Bean
    public JacksonModule jsonNullableJackson3Module() {
        return new JsonNullableJackson3Module();
    }
}
