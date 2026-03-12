/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Profile;

@Profile("mock-data-service")
@SpringBootApplication(scanBasePackages = {"com.akademiaplus"})
public class MockDataApp {
    public static void main(String[] args) {
        SpringApplication.run(MockDataApp.class, args);
    }
}