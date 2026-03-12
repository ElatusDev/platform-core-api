/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Profile;

/**
 * Entry point for the ETL System standalone service.
 *
 * <p>Runs on port 8280 and connects to MongoDB for staging data
 * and MariaDB for final entity loading.</p>
 */
@Profile("etl-service")
@SpringBootApplication(scanBasePackages = "com.akademiaplus")
public class EtlApp {

    public static void main(String[] args) {
        SpringApplication.run(EtlApp.class, args);
    }
}
