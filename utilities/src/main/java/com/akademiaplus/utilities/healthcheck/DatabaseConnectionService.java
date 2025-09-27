/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.utilities.healthcheck;

import com.akademiaplus.utilities.exceptions.DatabaseConnectionFailedException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

import org.springframework.retry.annotation.EnableRetry;

@Service
@EnableRetry
@ConditionalOnBean(DataSource.class)
public class DatabaseConnectionService {

    private final DataSource dataSource;

    public DatabaseConnectionService(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Retryable(maxAttempts = 5, backoff = @Backoff(delay = 20000))
    public Connection getConnection() throws SQLException {
        System.out.println("Attempting to get database connection..."); // Log this properly!
        return dataSource.getConnection();
    }

    public void performDatabaseOperation() {
        try (Connection connection = getConnection()) {
            // Use the connection for your database operations
        } catch (SQLException e) {
            throw new DatabaseConnectionFailedException("Failed to connect to the database", e); // Example
        }
    }
}

