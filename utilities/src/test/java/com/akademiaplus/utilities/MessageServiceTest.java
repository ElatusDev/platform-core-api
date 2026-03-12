/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.utilities;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;

import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link MessageService} generic methods.
 *
 * @author ElatusDev
 * @since 1.0
 */
@DisplayName("MessageService")
@ExtendWith(MockitoExtension.class)
class MessageServiceTest {

    private static final Locale LOCALE = Locale.forLanguageTag("es-MX");
    private static final String ENTITY_ID = "42";
    private static final String ENTITY_NAME_EMPLEADO = "Empleado";
    private static final String ENTITY_NAME_TUTOR = "Tutor";

    @Mock
    private MessageSource messageSource;

    private MessageService messageService;

    @BeforeEach
    void setUp() {
        messageService = new MessageService(messageSource);
    }

    @Nested
    @DisplayName("Generic Entity Messages")
    class GenericEntityMessages {

        @Test
        @DisplayName("Should format entity not found message with resolved entity name")
        void shouldFormatEntityNotFound_whenGivenEntityTypeAndId() {
            // Given
            when(messageSource.getMessage(EntityType.EMPLOYEE, null, LOCALE))
                    .thenReturn(ENTITY_NAME_EMPLEADO);
            when(messageSource.getMessage(MessageService.KEY_ENTITY_NOT_FOUND,
                    new Object[]{ENTITY_NAME_EMPLEADO, ENTITY_ID}, LOCALE))
                    .thenReturn("Empleado con ID: 42 no existe!");

            // When
            String result = messageService.getEntityNotFound(EntityType.EMPLOYEE, ENTITY_ID);

            // Then
            assertThat(result).isEqualTo("Empleado con ID: 42 no existe!");
            verify(messageSource, times(1)).getMessage(EntityType.EMPLOYEE, null, LOCALE);
            verify(messageSource, times(1)).getMessage(MessageService.KEY_ENTITY_NOT_FOUND,
                    new Object[]{ENTITY_NAME_EMPLEADO, ENTITY_ID}, LOCALE);
            verifyNoMoreInteractions(messageSource);
        }

        @Test
        @DisplayName("Should format entity delete not allowed message for DB constraint")
        void shouldFormatDeleteNotAllowed_whenGivenEntityType() {
            // Given
            when(messageSource.getMessage(EntityType.EMPLOYEE, null, LOCALE))
                    .thenReturn(ENTITY_NAME_EMPLEADO);
            when(messageSource.getMessage(MessageService.KEY_ENTITY_DELETE_NOT_ALLOWED,
                    new Object[]{ENTITY_NAME_EMPLEADO}, LOCALE))
                    .thenReturn("Eliminacion de Empleado no esta permitida!...");

            // When
            String result = messageService.getEntityDeleteNotAllowed(EntityType.EMPLOYEE);

            // Then
            assertThat(result).isEqualTo("Eliminacion de Empleado no esta permitida!...");
            verify(messageSource, times(1)).getMessage(EntityType.EMPLOYEE, null, LOCALE);
            verify(messageSource, times(1)).getMessage(MessageService.KEY_ENTITY_DELETE_NOT_ALLOWED,
                    new Object[]{ENTITY_NAME_EMPLEADO}, LOCALE);
            verifyNoMoreInteractions(messageSource);
        }

        @Test
        @DisplayName("Should format entity delete not allowed message with business reason")
        void shouldFormatDeleteNotAllowedWithReason_whenGivenReasonString() {
            // Given
            String reason = "Tutor tiene 3 alumno(s) menor(es) activo(s)";
            when(messageSource.getMessage(EntityType.TUTOR, null, LOCALE))
                    .thenReturn(ENTITY_NAME_TUTOR);
            when(messageSource.getMessage(MessageService.KEY_ENTITY_DELETE_NOT_ALLOWED_REASON,
                    new Object[]{ENTITY_NAME_TUTOR, "7", reason}, LOCALE))
                    .thenReturn("Eliminacion de Tutor con ID: 7 no es posible: " + reason);

            // When
            String result = messageService.getEntityDeleteNotAllowedWithReason(
                    EntityType.TUTOR, "7", reason);

            // Then
            assertThat(result).startsWith("Eliminacion de Tutor con ID: 7 no es posible:");
            verify(messageSource, times(1)).getMessage(EntityType.TUTOR, null, LOCALE);
            verify(messageSource, times(1)).getMessage(MessageService.KEY_ENTITY_DELETE_NOT_ALLOWED_REASON,
                    new Object[]{ENTITY_NAME_TUTOR, "7", reason}, LOCALE);
            verifyNoMoreInteractions(messageSource);
        }

        @Test
        @DisplayName("Should format duplicate entity field message")
        void shouldFormatDuplicateField_whenGivenEntityTypeAndField() {
            // Given
            when(messageSource.getMessage(EntityType.EMPLOYEE, null, LOCALE))
                    .thenReturn(ENTITY_NAME_EMPLEADO);
            when(messageSource.getMessage(MessageService.KEY_ENTITY_DUPLICATE_FIELD,
                    new Object[]{ENTITY_NAME_EMPLEADO, "email"}, LOCALE))
                    .thenReturn("Error: el campo email del Empleado ya esta registrado");

            // When
            String result = messageService.getEntityDuplicateField(EntityType.EMPLOYEE, "email");

            // Then
            assertThat(result).isEqualTo("Error: el campo email del Empleado ya esta registrado");
            verify(messageSource, times(1)).getMessage(EntityType.EMPLOYEE, null, LOCALE);
            verify(messageSource, times(1)).getMessage(MessageService.KEY_ENTITY_DUPLICATE_FIELD,
                    new Object[]{ENTITY_NAME_EMPLEADO, "email"}, LOCALE);
            verifyNoMoreInteractions(messageSource);
        }

        @Test
        @DisplayName("Should return constraint violation message")
        void shouldReturnConstraintViolation_whenCalled() {
            // Given
            when(messageSource.getMessage(MessageService.KEY_ENTITY_CONSTRAINT_VIOLATION, null, LOCALE))
                    .thenReturn("Operacion no permitida: conflicto de integridad de datos");

            // When
            String result = messageService.getConstraintViolation();

            // Then
            assertThat(result).isEqualTo("Operacion no permitida: conflicto de integridad de datos");
            verify(messageSource, times(1)).getMessage(MessageService.KEY_ENTITY_CONSTRAINT_VIOLATION, null, LOCALE);
            verifyNoMoreInteractions(messageSource);
        }

        @Test
        @DisplayName("Should return invalid tenant message")
        void shouldReturnInvalidTenantMessage_whenCalled() {
            // Given
            when(messageSource.getMessage(MessageService.KEY_INVALID_TENANT, null, LOCALE))
                    .thenReturn("Contexto de organizacion es requerido");

            // When
            String result = messageService.getInvalidTenant();

            // Then
            assertThat(result).isEqualTo("Contexto de organizacion es requerido");
            verify(messageSource, times(1)).getMessage(MessageService.KEY_INVALID_TENANT, null, LOCALE);
            verifyNoMoreInteractions(messageSource);
        }
    }
}
