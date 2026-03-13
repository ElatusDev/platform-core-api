/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.employee.usecases;

import com.akademiaplus.employee.interfaceadapters.EmployeeRepository;
import com.akademiaplus.utilities.exceptions.EntityNotFoundException;
import com.akademiaplus.utilities.EntityType;
import com.akademiaplus.infra.persistence.config.TenantContextHolder;
import com.akademiaplus.users.base.PersonPIIDataModel;
import com.akademiaplus.users.employee.EmployeeDataModel;
import openapi.akademiaplus.domain.user.management.dto.GetEmployeeResponseDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@DisplayName("GetEmployeeByIdUseCase")
@ExtendWith(MockitoExtension.class)
class GetEmployeeByIdUseCaseTest {

    private static final Long TENANT_ID = 1L;
    private static final Long EMPLOYEE_ID = 100L;

    @Mock private EmployeeRepository employeeRepository;
    @Mock private TenantContextHolder tenantContextHolder;
    @Mock private ModelMapper modelMapper;

    private GetEmployeeByIdUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new GetEmployeeByIdUseCase(employeeRepository, tenantContextHolder, modelMapper);
    }

    @Nested
    @DisplayName("Retrieval")
    class Retrieval {

        @Test
        @DisplayName("Should return mapped DTO when employee is found")
        void shouldReturnMappedDto_whenEmployeeFound() {
            // Given
            PersonPIIDataModel personPII = new PersonPIIDataModel();
            EmployeeDataModel employee = new EmployeeDataModel();
            employee.setPersonPII(personPII);
            GetEmployeeResponseDTO expectedDto = new GetEmployeeResponseDTO();

            when(tenantContextHolder.getTenantId()).thenReturn(Optional.of(TENANT_ID));
            when(employeeRepository.findById(new EmployeeDataModel.EmployeeCompositeId(TENANT_ID, EMPLOYEE_ID)))
                    .thenReturn(Optional.of(employee));
            when(modelMapper.map(employee, GetEmployeeResponseDTO.class)).thenReturn(expectedDto);

            // When
            GetEmployeeResponseDTO result = useCase.get(EMPLOYEE_ID);

            // Then
            assertThat(result).isEqualTo(expectedDto);
            verify(tenantContextHolder, times(1)).getTenantId();
            verify(employeeRepository, times(1)).findById(new EmployeeDataModel.EmployeeCompositeId(TENANT_ID, EMPLOYEE_ID));
            verify(modelMapper, times(1)).map(employee, GetEmployeeResponseDTO.class);
            verify(modelMapper, times(1)).map(personPII, expectedDto);
            verifyNoMoreInteractions(tenantContextHolder, employeeRepository, modelMapper);
        }
    }

    @Nested
    @DisplayName("Not found")
    class NotFound {

        @Test
        @DisplayName("Should throw EntityNotFoundException when employee not found")
        void shouldThrowEntityNotFoundException_whenEmployeeNotFound() {
            // Given
            when(tenantContextHolder.getTenantId()).thenReturn(Optional.of(TENANT_ID));
            when(employeeRepository.findById(new EmployeeDataModel.EmployeeCompositeId(TENANT_ID, EMPLOYEE_ID)))
                    .thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> useCase.get(EMPLOYEE_ID))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessage(String.format(EntityNotFoundException.MESSAGE_TEMPLATE,
                            EntityType.EMPLOYEE, EMPLOYEE_ID))
                    .satisfies(ex -> {
                        EntityNotFoundException enfe = (EntityNotFoundException) ex;
                        assertThat(enfe.getEntityType()).isEqualTo(EntityType.EMPLOYEE);
                        assertThat(enfe.getEntityId()).isEqualTo(String.valueOf(EMPLOYEE_ID));
                    });
            verify(tenantContextHolder, times(1)).getTenantId();
            verify(employeeRepository, times(1)).findById(new EmployeeDataModel.EmployeeCompositeId(TENANT_ID, EMPLOYEE_ID));
            verifyNoInteractions(modelMapper);
            verifyNoMoreInteractions(tenantContextHolder, employeeRepository);
        }
    }

    @Nested
    @DisplayName("Tenant context")
    class TenantContext {

        @Test
        @DisplayName("Should throw IllegalArgumentException when tenant context is missing")
        void shouldThrowIllegalArgumentException_whenTenantContextMissing() {
            // Given
            when(tenantContextHolder.getTenantId()).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> useCase.get(EMPLOYEE_ID))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage(GetEmployeeByIdUseCase.ERROR_TENANT_CONTEXT_REQUIRED);
            verify(tenantContextHolder, times(1)).getTenantId();
            verifyNoMoreInteractions(tenantContextHolder);
            verifyNoInteractions(employeeRepository, modelMapper);
        }
    }
}
