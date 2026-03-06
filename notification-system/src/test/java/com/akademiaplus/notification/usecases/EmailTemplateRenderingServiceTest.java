/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.notification.usecases;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("EmailTemplateRenderingService")
class EmailTemplateRenderingServiceTest {

    private static final String SIMPLE_TEMPLATE = "Hello {{name}}, welcome!";
    private static final String MULTI_VAR_TEMPLATE = "Dear {{firstName}} {{lastName}}, your course {{courseName}} starts soon.";
    private static final String UNKNOWN_VAR_TEMPLATE = "Hello {{name}}, your code is {{code}}.";
    private static final String NO_VARS_TEMPLATE = "This is a plain text email with no variables.";
    private static final String SPECIAL_CHARS_TEMPLATE = "Price: {{price}}, path: {{path}}";

    private static final String VAR_NAME = "name";
    private static final String VAR_FIRST_NAME = "firstName";
    private static final String VAR_LAST_NAME = "lastName";
    private static final String VAR_COURSE_NAME = "courseName";
    private static final String VAR_CODE = "code";
    private static final String VAR_PRICE = "price";
    private static final String VAR_PATH = "path";

    private static final String VALUE_NAME = "Carlos";
    private static final String VALUE_FIRST_NAME = "Maria";
    private static final String VALUE_LAST_NAME = "Garcia";
    private static final String VALUE_COURSE_NAME = "Advanced Java";
    private static final String VALUE_PRICE = "$99.99";
    private static final String VALUE_PATH = "C:\\Users\\docs";

    private EmailTemplateRenderingService service;

    @BeforeEach
    void setUp() {
        service = new EmailTemplateRenderingService();
    }

    @Nested
    @DisplayName("Render")
    class Render {

        @Test
        @DisplayName("Should replace a simple variable placeholder with its value")
        void shouldReplaceSimpleVariable() {
            // Given
            Map<String, Object> variables = Map.of(VAR_NAME, VALUE_NAME);

            // When
            String result = service.render(SIMPLE_TEMPLATE, variables);

            // Then
            assertThat(result).isEqualTo("Hello Carlos, welcome!");
        }

        @Test
        @DisplayName("Should replace multiple variable placeholders with their values")
        void shouldReplaceMultipleVariables() {
            // Given
            Map<String, Object> variables = Map.of(
                    VAR_FIRST_NAME, VALUE_FIRST_NAME,
                    VAR_LAST_NAME, VALUE_LAST_NAME,
                    VAR_COURSE_NAME, VALUE_COURSE_NAME
            );

            // When
            String result = service.render(MULTI_VAR_TEMPLATE, variables);

            // Then
            assertThat(result).isEqualTo("Dear Maria Garcia, your course Advanced Java starts soon.");
        }

        @Test
        @DisplayName("Should leave unknown variable placeholders untouched")
        void shouldLeaveUnknownVariablesUntouched() {
            // Given
            Map<String, Object> variables = Map.of(VAR_NAME, VALUE_NAME);

            // When
            String result = service.render(UNKNOWN_VAR_TEMPLATE, variables);

            // Then
            assertThat(result).isEqualTo("Hello Carlos, your code is {{code}}.");
        }

        @Test
        @DisplayName("Should return template as-is when variables map is empty")
        void shouldReturnTemplateAsIs_whenVariablesEmpty() {
            // Given
            Map<String, Object> variables = Collections.emptyMap();

            // When
            String result = service.render(NO_VARS_TEMPLATE, variables);

            // Then
            assertThat(result).isEqualTo(NO_VARS_TEMPLATE);
        }

        @Test
        @DisplayName("Should return null when template is null")
        void shouldReturnNull_whenTemplateNull() {
            // Given
            Map<String, Object> variables = Map.of(VAR_NAME, VALUE_NAME);

            // When
            String result = service.render(null, variables);

            // Then
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("Should handle special characters in variable values")
        void shouldHandleSpecialCharactersInValues() {
            // Given
            Map<String, Object> variables = Map.of(
                    VAR_PRICE, VALUE_PRICE,
                    VAR_PATH, VALUE_PATH
            );

            // When
            String result = service.render(SPECIAL_CHARS_TEMPLATE, variables);

            // Then
            assertThat(result).isEqualTo("Price: $99.99, path: C:\\Users\\docs");
        }
    }
}
