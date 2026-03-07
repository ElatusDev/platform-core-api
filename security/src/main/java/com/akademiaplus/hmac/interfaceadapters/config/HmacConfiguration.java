/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.hmac.interfaceadapters.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Spring configuration that enables HMAC signing properties.
 *
 * @author ElatusDev
 * @since 1.0
 */
@Configuration
@EnableConfigurationProperties(HmacProperties.class)
public class HmacConfiguration {
}
