/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.employee.usecases;

import com.akademiaplus.employee.interfaceadapters.EmployeeRepository;
import com.akademiaplus.infra.persistence.config.TenantContextHolder;
import com.akademiaplus.users.employee.EmployeeDataModel;
import com.akademiaplus.utilities.EntityType;
import com.akademiaplus.utilities.exceptions.EntityDeletionNotAllowedException;
import com.akademiaplus.utilities.exceptions.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link DeleteEmployeeUseCase}.
 *
 * @author ElatusDev
 * @since 1.0
 */
@DisplayName("DeleteEmployeeUseCase")
@ExtendWith(MockitoExtension.class)
class DeleteEmployeeUseCaseTest {

    private static final Long TENANT_ID = 1L;
    private static final Long EMPLOYEE_ID = 42L;

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private TenantContextHolder tenantContextHolder;

    private DeleteEmployeeUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new DeleteEmployeeUseCase(employeeRepository, tenantContextHolder);
    }

    @Nested
    @DisplayName("Successful Deletion")
    class SuccessfulDeletion {

        @Test
        @DisplayName("Should soft-delete employee when found by composite key")
        void shouldSoftDeleteEmployee_whenFoundByCompositeKey() {
            // Given
            when(tenantContextHolder.requireTenantId()).thenReturn(TENANT_ID);
            EmployeeDataModel entity = new EmployeeDataModel();
            EmployeeDataModel.EmployeeCompositeId compositeId =
                    new EmployeeDataModel.EmployeeCompositeId(TENANT_ID, EMPLOYEE_ID);
            when(employeeRepository.findById(compositeId)).thenReturn(Optional.of(entity));

            // When
            useCase.delete(EMPLOYEE_ID);

            // Then
            verify(employeeRepository).delete(entity);
        }
    }

    @Nested
    @DisplayName("Entity Not Found")
    class EntityNotFound {

        @Test
        @DisplayName("Should throw EntityNotFoundException when employee missing")
        void shouldThrowEntityNotFound_whenEmployeeMissing() {
            // Given
            when(tenantContextHolder.requireTenantId()).thenReturn(TENANT_ID);
            EmployeeDataModel.EmployeeCompositeId compositeId =
                    new EmployeeDataModel.EmployeeCompositeId(TENANT_ID, EMPLOYEE_ID);
            when(employeeRepository.findById(compositeId)).thenReturn(Optional.empty());

            // When / Then
            assertThatThrownBy(() -> useCase.delete(EMPLOYEE_ID))
                    .isInstanceOf(EntityNotFoundException.class)
                    .satisfies(ex -> {
                        EntityNotFoundException enfe = (EntityNotFoundException) ex;
                        assertThat(enfe.getEntityType()).isEqualTo(EntityType.EMPLOYEE);
                        assertThat(enfe.getEntityId()).isEqualTo(String.valueOf(EMPLOYEE_ID));
                    });
        }
    }

    @Nested
    @DisplayName("Constraint Violation")
    class ConstraintViolation {

        @Test
        @DisplayName("Should throw EntityDeletionNotAllowed when constraint violated")
        void shouldThrowDeletionNotAllowed_whenConstraintViolated() {
            // Given
            when(tenantContextHolder.requireTenantId()).thenReturn(TENANT_ID);
            EmployeeDataModel entity = new EmployeeDataModel();
            EmployeeDataModel.EmployeeCompositeId compositeId =
                    new EmployeeDataModel.EmployeeCompositeId(TENANT_ID, EMPLOYEE_ID);
            when(employeeRepository.findById(compositeId)).thenReturn(Optional.of(entity));
            doThrow(new DataIntegrityViolationException("FK constraint"))
                    .when(employeeRepository).delete(entity);

            // When / Then
            assertThatThrownBy(() -> useCase.delete(EMPLOYEE_ID))
                    .isInstanceOf(EntityDeletionNotAllowedException.class)
                    .satisfies(ex -> {
                        EntityDeletionNotAllowedException edna =
                                (EntityDeletionNotAllowedException) ex;
                        assertThat(edna.getEntityType()).isEqualTo(EntityType.EMPLOYEE);
                        assertThat(edna.getEntityId()).isEqualTo(String.valueOf(EMPLOYEE_ID));
                        assertThat(edna.getReason()).isNull();
                    });
        }
    }
}
