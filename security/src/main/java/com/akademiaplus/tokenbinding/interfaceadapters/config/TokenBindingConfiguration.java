/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.tokenbinding.interfaceadapters.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Spring configuration that enables token binding properties.
 *
 * @author ElatusDev
 * @since 1.0
 */
@Configuration
@EnableConfigurationProperties(TokenBindingProperties.class)
public class TokenBindingConfiguration {
}
