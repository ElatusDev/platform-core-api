/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.makani;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Profile;
import org.springframework.context.annotation.PropertySource;

@Profile("mock-data-service")
@PropertySource("classpath:mock-data-service.properties")
@SpringBootApplication(scanBasePackages = {"com.makani"})
public class MockDataApp {
    public static void main(String[] args) {
        SpringApplication.run(MockDataApp.class, args);
    }
}