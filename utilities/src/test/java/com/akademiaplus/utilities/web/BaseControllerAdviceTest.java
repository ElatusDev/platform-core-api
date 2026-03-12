/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.utilities.web;

import com.akademiaplus.utilities.EntityType;
import com.akademiaplus.utilities.MessageService;
import com.akademiaplus.utilities.exceptions.DuplicateEntityException;
import com.akademiaplus.utilities.exceptions.EntityDeletionNotAllowedException;
import com.akademiaplus.utilities.exceptions.EntityNotFoundException;
import com.akademiaplus.utilities.exceptions.InvalidTenantException;
import com.akademiaplus.utilities.exceptions.security.DecryptionFailureException;
import com.akademiaplus.utilities.exceptions.security.EncryptionFailureException;
import openapi.akademiaplus.domain.utilities.dto.ErrorResponseDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link BaseControllerAdvice}.
 *
 * @author ElatusDev
 * @since 1.0
 */
@DisplayName("BaseControllerAdvice")
@ExtendWith(MockitoExtension.class)
class BaseControllerAdviceTest {

    private static final String ENTITY_ID = "42";
    private static final String NOT_FOUND_MESSAGE = "Empleado con ID: 42 no existe!";
    private static final String DELETE_CONSTRAINT_MESSAGE = "Eliminacion de Empleado no esta permitida!";
    private static final String BUSINESS_REASON = "Tutor tiene 3 alumno(s) menor(es) activo(s)";
    private static final String DELETE_REASON_MESSAGE = "Eliminacion de Tutor con ID: 7 no es posible: " + BUSINESS_REASON;
    private static final String DUPLICATE_MESSAGE = "Error: el campo email del Empleado ya esta registrado";
    private static final String CONSTRAINT_MESSAGE = "Operacion no permitida: conflicto de integridad de datos";
    private static final String INVALID_TENANT_MESSAGE = "Contexto de organizacion es requerido";
    private static final String INTERNAL_ERROR_MESSAGE = "Error interno en la applicacion";

    /**
     * Concrete test subclass — no additional handlers.
     */
    private static class TestControllerAdvice extends BaseControllerAdvice {
        TestControllerAdvice(MessageService messageService) {
            super(messageService);
        }
    }

    @Mock
    private MessageService messageService;

    private TestControllerAdvice advice;

    @BeforeEach
    void setUp() {
        advice = new TestControllerAdvice(messageService);
    }

    @Nested
    @DisplayName("EntityNotFoundException Handling")
    class EntityNotFoundHandling {

        @Test
        @DisplayName("Should return 404 when EntityNotFoundException thrown")
        void shouldReturn404_whenEntityNotFoundThrown() {
            // Given
            EntityNotFoundException ex = new EntityNotFoundException(EntityType.EMPLOYEE, ENTITY_ID);
            when(messageService.getEntityNotFound(EntityType.EMPLOYEE, ENTITY_ID))
                    .thenReturn(NOT_FOUND_MESSAGE);

            // When
            ResponseEntity<ErrorResponseDTO> response = advice.handleEntityNotFound(ex);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
            verify(messageService, times(1)).getEntityNotFound(EntityType.EMPLOYEE, ENTITY_ID);
            verifyNoMoreInteractions(messageService);
        }

        @Test
        @DisplayName("Should set code to ENTITY_NOT_FOUND when handled")
        void shouldSetCodeToEntityNotFound_whenHandled() {
            // Given
            EntityNotFoundException ex = new EntityNotFoundException(EntityType.EMPLOYEE, ENTITY_ID);
            when(messageService.getEntityNotFound(EntityType.EMPLOYEE, ENTITY_ID))
                    .thenReturn(NOT_FOUND_MESSAGE);

            // When
            ResponseEntity<ErrorResponseDTO> response = advice.handleEntityNotFound(ex);

            // Then
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getCode()).isEqualTo(BaseControllerAdvice.CODE_ENTITY_NOT_FOUND);
            verify(messageService, times(1)).getEntityNotFound(EntityType.EMPLOYEE, ENTITY_ID);
            verifyNoMoreInteractions(messageService);
        }

        @Test
        @DisplayName("Should delegate message to MessageService when handled")
        void shouldDelegateMessageToMessageService_whenHandled() {
            // Given
            EntityNotFoundException ex = new EntityNotFoundException(EntityType.EMPLOYEE, ENTITY_ID);
            when(messageService.getEntityNotFound(EntityType.EMPLOYEE, ENTITY_ID))
                    .thenReturn(NOT_FOUND_MESSAGE);

            // When
            ResponseEntity<ErrorResponseDTO> response = advice.handleEntityNotFound(ex);

            // Then
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getMessage()).isEqualTo(NOT_FOUND_MESSAGE);
            verify(messageService, times(1)).getEntityNotFound(EntityType.EMPLOYEE, ENTITY_ID);
            verifyNoMoreInteractions(messageService);
        }
    }

    @Nested
    @DisplayName("EntityDeletionNotAllowed Handling")
    class DeletionNotAllowedHandling {

        @Test
        @DisplayName("Should return 409 with constraint code when cause is present")
        void shouldReturn409WithConstraintCode_whenCauseIsPresent() {
            // Given
            Throwable cause = new RuntimeException("FK violation");
            EntityDeletionNotAllowedException ex =
                    new EntityDeletionNotAllowedException(EntityType.EMPLOYEE, ENTITY_ID, cause);
            when(messageService.getEntityDeleteNotAllowed(EntityType.EMPLOYEE))
                    .thenReturn(DELETE_CONSTRAINT_MESSAGE);

            // When
            ResponseEntity<ErrorResponseDTO> response = advice.handleDeletionNotAllowed(ex);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getCode())
                    .isEqualTo(BaseControllerAdvice.CODE_DELETION_CONSTRAINT_VIOLATION);
            verify(messageService, times(1)).getEntityDeleteNotAllowed(EntityType.EMPLOYEE);
            verifyNoMoreInteractions(messageService);
        }

        @Test
        @DisplayName("Should return 409 with business rule code when reason is present")
        void shouldReturn409WithBusinessRuleCode_whenReasonIsPresent() {
            // Given
            EntityDeletionNotAllowedException ex =
                    new EntityDeletionNotAllowedException(EntityType.TUTOR, "7", BUSINESS_REASON);
            when(messageService.getEntityDeleteNotAllowedWithReason(
                    EntityType.TUTOR, "7", BUSINESS_REASON))
                    .thenReturn(DELETE_REASON_MESSAGE);

            // When
            ResponseEntity<ErrorResponseDTO> response = advice.handleDeletionNotAllowed(ex);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getCode())
                    .isEqualTo(BaseControllerAdvice.CODE_DELETION_BUSINESS_RULE);
            verify(messageService, times(1)).getEntityDeleteNotAllowedWithReason(
                    EntityType.TUTOR, "7", BUSINESS_REASON);
            verifyNoMoreInteractions(messageService);
        }

        @Test
        @DisplayName("Should delegate constraint message when cause variant")
        void shouldDelegateConstraintMessage_whenCauseVariant() {
            // Given
            Throwable cause = new RuntimeException("FK violation");
            EntityDeletionNotAllowedException ex =
                    new EntityDeletionNotAllowedException(EntityType.EMPLOYEE, ENTITY_ID, cause);
            when(messageService.getEntityDeleteNotAllowed(EntityType.EMPLOYEE))
                    .thenReturn(DELETE_CONSTRAINT_MESSAGE);

            // When
            ResponseEntity<ErrorResponseDTO> response = advice.handleDeletionNotAllowed(ex);

            // Then
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getMessage()).isEqualTo(DELETE_CONSTRAINT_MESSAGE);
            verify(messageService, times(1)).getEntityDeleteNotAllowed(EntityType.EMPLOYEE);
            verifyNoMoreInteractions(messageService);
        }

        @Test
        @DisplayName("Should delegate reason message when reason variant")
        void shouldDelegateReasonMessage_whenReasonVariant() {
            // Given
            EntityDeletionNotAllowedException ex =
                    new EntityDeletionNotAllowedException(EntityType.TUTOR, "7", BUSINESS_REASON);
            when(messageService.getEntityDeleteNotAllowedWithReason(
                    EntityType.TUTOR, "7", BUSINESS_REASON))
                    .thenReturn(DELETE_REASON_MESSAGE);

            // When
            ResponseEntity<ErrorResponseDTO> response = advice.handleDeletionNotAllowed(ex);

            // Then
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getMessage()).isEqualTo(DELETE_REASON_MESSAGE);
            verify(messageService, times(1)).getEntityDeleteNotAllowedWithReason(
                    EntityType.TUTOR, "7", BUSINESS_REASON);
            verifyNoMoreInteractions(messageService);
        }
    }

    @Nested
    @DisplayName("DuplicateEntity Handling")
    class DuplicateEntityHandling {

        @Test
        @DisplayName("Should return 409 when DuplicateEntityException thrown")
        void shouldReturn409_whenDuplicateEntityThrown() {
            // Given
            DuplicateEntityException ex = new DuplicateEntityException(
                    EntityType.EMPLOYEE, "email", new RuntimeException("unique constraint"));
            when(messageService.getEntityDuplicateField(EntityType.EMPLOYEE, "email"))
                    .thenReturn(DUPLICATE_MESSAGE);

            // When
            ResponseEntity<ErrorResponseDTO> response = advice.handleDuplicateEntity(ex);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
            verify(messageService, times(1)).getEntityDuplicateField(EntityType.EMPLOYEE, "email");
            verifyNoMoreInteractions(messageService);
        }

        @Test
        @DisplayName("Should set code to DUPLICATE_ENTITY when handled")
        void shouldSetCodeToDuplicateEntity_whenHandled() {
            // Given
            DuplicateEntityException ex = new DuplicateEntityException(
                    EntityType.EMPLOYEE, "email", new RuntimeException("unique constraint"));
            when(messageService.getEntityDuplicateField(EntityType.EMPLOYEE, "email"))
                    .thenReturn(DUPLICATE_MESSAGE);

            // When
            ResponseEntity<ErrorResponseDTO> response = advice.handleDuplicateEntity(ex);

            // Then
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getCode()).isEqualTo(BaseControllerAdvice.CODE_DUPLICATE_ENTITY);
            assertThat(response.getBody().getMessage()).isEqualTo(DUPLICATE_MESSAGE);
            verify(messageService, times(1)).getEntityDuplicateField(EntityType.EMPLOYEE, "email");
            verifyNoMoreInteractions(messageService);
        }
    }

    @Nested
    @DisplayName("DataIntegrityViolation Handling")
    class DataIntegrityViolationHandling {

        @Test
        @DisplayName("Should return 409 when DataIntegrityViolationException thrown")
        void shouldReturn409_whenDataIntegrityViolationThrown() {
            // Given
            DataIntegrityViolationException ex = new DataIntegrityViolationException("constraint");
            when(messageService.getConstraintViolation()).thenReturn(CONSTRAINT_MESSAGE);

            // When
            ResponseEntity<ErrorResponseDTO> response = advice.handleDataIntegrityViolation(ex);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getCode())
                    .isEqualTo(BaseControllerAdvice.CODE_DATA_INTEGRITY_VIOLATION);
            verify(messageService, times(1)).getConstraintViolation();
            verifyNoMoreInteractions(messageService);
        }

        @Test
        @DisplayName("Should not throw NPE when cause chain is null")
        void shouldNotThrowNPE_whenCauseChainIsNull() {
            // Given
            DataIntegrityViolationException ex = new DataIntegrityViolationException("no cause");
            when(messageService.getConstraintViolation()).thenReturn(CONSTRAINT_MESSAGE);

            // When
            ResponseEntity<ErrorResponseDTO> response = advice.handleDataIntegrityViolation(ex);

            // Then
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getMessage()).isEqualTo(CONSTRAINT_MESSAGE);
            verify(messageService, times(1)).getConstraintViolation();
            verifyNoMoreInteractions(messageService);
        }
    }

    @Nested
    @DisplayName("Validation Handling")
    class ValidationHandling {

        @Test
        @DisplayName("Should return 400 when validation fails")
        void shouldReturn400_whenValidationFails() {
            // Given
            BindingResult bindingResult = mock(BindingResult.class);
            FieldError fieldError = new FieldError("request", "email", "must not be blank");
            when(bindingResult.getFieldErrors()).thenReturn(List.of(fieldError));
            MethodArgumentNotValidException ex =
                    new MethodArgumentNotValidException(null, bindingResult);

            // When
            ResponseEntity<ErrorResponseDTO> response = advice.handleValidation(ex);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getCode()).isEqualTo(BaseControllerAdvice.CODE_VALIDATION_ERROR);
            verifyNoInteractions(messageService);
        }

        @Test
        @DisplayName("Should populate details array when field errors exist")
        void shouldPopulateDetailsArray_whenFieldErrorsExist() {
            // Given
            BindingResult bindingResult = mock(BindingResult.class);
            FieldError emailError = new FieldError("request", "email", "must not be blank");
            FieldError birthDateError = new FieldError("request", "birthDate", "must not be null");
            when(bindingResult.getFieldErrors()).thenReturn(List.of(emailError, birthDateError));
            MethodArgumentNotValidException ex =
                    new MethodArgumentNotValidException(null, bindingResult);

            // When
            ResponseEntity<ErrorResponseDTO> response = advice.handleValidation(ex);

            // Then
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getDetails()).hasSize(2);
            assertThat(response.getBody().getDetails().get(0).getField()).isEqualTo("email");
            assertThat(response.getBody().getDetails().get(0).getMessage()).isEqualTo("must not be blank");
            assertThat(response.getBody().getDetails().get(1).getField()).isEqualTo("birthDate");
            verifyNoInteractions(messageService);
        }
    }

    @Nested
    @DisplayName("InvalidTenant Handling")
    class InvalidTenantHandling {

        @Test
        @DisplayName("Should return 400 when InvalidTenantException thrown")
        void shouldReturn400_whenInvalidTenantThrown() {
            // Given
            InvalidTenantException ex = new InvalidTenantException("Tenant context is required");
            when(messageService.getInvalidTenant()).thenReturn(INVALID_TENANT_MESSAGE);

            // When
            ResponseEntity<ErrorResponseDTO> response = advice.handleInvalidTenant(ex);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getCode()).isEqualTo(BaseControllerAdvice.CODE_INVALID_TENANT);
            assertThat(response.getBody().getMessage()).isEqualTo(INVALID_TENANT_MESSAGE);
            verify(messageService, times(1)).getInvalidTenant();
            verifyNoMoreInteractions(messageService);
        }
    }

    @Nested
    @DisplayName("CryptoFailure Handling")
    class CryptoFailureHandling {

        @Test
        @DisplayName("Should return 500 when EncryptionFailureException thrown")
        void shouldReturn500_whenEncryptionFailureThrown() {
            // Given
            EncryptionFailureException ex = new EncryptionFailureException("AES failed",
                    new RuntimeException("key error"));
            when(messageService.getInternalErrorHighSeverity()).thenReturn(INTERNAL_ERROR_MESSAGE);

            // When
            ResponseEntity<ErrorResponseDTO> response = advice.handleCryptoFailure(ex);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getCode()).isEqualTo(BaseControllerAdvice.CODE_INTERNAL_ERROR);
            verify(messageService, times(1)).getInternalErrorHighSeverity();
            verifyNoMoreInteractions(messageService);
        }

        @Test
        @DisplayName("Should return 500 when DecryptionFailureException thrown")
        void shouldReturn500_whenDecryptionFailureThrown() {
            // Given
            DecryptionFailureException ex = new DecryptionFailureException("AES failed",
                    new RuntimeException("iv error"));
            when(messageService.getInternalErrorHighSeverity()).thenReturn(INTERNAL_ERROR_MESSAGE);

            // When
            ResponseEntity<ErrorResponseDTO> response = advice.handleCryptoFailure(ex);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getMessage()).isEqualTo(INTERNAL_ERROR_MESSAGE);
            verify(messageService, times(1)).getInternalErrorHighSeverity();
            verifyNoMoreInteractions(messageService);
        }
    }

    @Nested
    @DisplayName("Fallback Handling")
    class FallbackHandling {

        @Test
        @DisplayName("Should return 500 when unexpected exception thrown")
        void shouldReturn500_whenUnexpectedExceptionThrown() {
            // Given
            Exception ex = new RuntimeException("Something unexpected");
            when(messageService.getInternalErrorHighSeverity()).thenReturn(INTERNAL_ERROR_MESSAGE);

            // When
            ResponseEntity<ErrorResponseDTO> response = advice.handleUnexpected(ex);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getCode()).isEqualTo(BaseControllerAdvice.CODE_INTERNAL_ERROR);
            assertThat(response.getBody().getMessage()).isEqualTo(INTERNAL_ERROR_MESSAGE);
            verify(messageService, times(1)).getInternalErrorHighSeverity();
            verifyNoMoreInteractions(messageService);
        }
    }

    @Nested
    @DisplayName("Collaborator exception propagation")
    class CollaboratorExceptionPropagation {

        @Test
        @DisplayName("Should propagate exception when messageService.getEntityNotFound throws")
        void shouldPropagateException_whenMessageServiceGetEntityNotFoundThrows() {
            // Given: messageService throws on entity name resolution
            EntityNotFoundException ex = new EntityNotFoundException(EntityType.EMPLOYEE, ENTITY_ID);
            RuntimeException messageError = new RuntimeException("Message resolution failed");
            when(messageService.getEntityNotFound(EntityType.EMPLOYEE, ENTITY_ID))
                    .thenThrow(messageError);

            // When/Then: exception should propagate
            assertThatThrownBy(() -> advice.handleEntityNotFound(ex))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Message resolution failed");

            verify(messageService, times(1)).getEntityNotFound(EntityType.EMPLOYEE, ENTITY_ID);
            verifyNoMoreInteractions(messageService);
        }

        @Test
        @DisplayName("Should propagate exception when messageService.getInternalErrorHighSeverity throws")
        void shouldPropagateException_whenMessageServiceGetInternalErrorHighSeverityThrows() {
            // Given: messageService throws on internal error resolution
            EncryptionFailureException ex = new EncryptionFailureException("AES failed",
                    new RuntimeException("key error"));
            RuntimeException messageError = new RuntimeException("Message resolution failed");
            when(messageService.getInternalErrorHighSeverity())
                    .thenThrow(messageError);

            // When/Then: exception should propagate
            assertThatThrownBy(() -> advice.handleCryptoFailure(ex))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Message resolution failed");

            verify(messageService, times(1)).getInternalErrorHighSeverity();
            verifyNoMoreInteractions(messageService);
        }
    }
}
