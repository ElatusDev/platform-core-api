/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.employee.usecases;

import com.akademiaplus.employee.interfaceadapters.EmployeeRepository;
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

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@DisplayName("GetAllEmployeesUseCase")
@ExtendWith(MockitoExtension.class)
class GetAllEmployeesUseCaseTest {

    @Mock private EmployeeRepository employeeRepository;
    @Mock private ModelMapper modelMapper;

    private GetAllEmployeesUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new GetAllEmployeesUseCase(employeeRepository, modelMapper);
    }

    @Nested
    @DisplayName("Retrieval")
    class Retrieval {

        @Test
        @DisplayName("Should return empty list when no employees exist")
        void shouldReturnEmptyList_whenNoEmployeesExist() {
            // Given
            when(employeeRepository.findAll()).thenReturn(Collections.emptyList());

            // When
            List<GetEmployeeResponseDTO> result = useCase.getAll();

            // Then
            assertThat(result).isEmpty();
            verify(employeeRepository, times(1)).findAll();
            verifyNoMoreInteractions(employeeRepository, modelMapper);
        }

        @Test
        @DisplayName("Should return mapped DTOs when employees exist")
        void shouldReturnMappedDtos_whenEmployeesExist() {
            // Given
            PersonPIIDataModel personPII1 = new PersonPIIDataModel();
            PersonPIIDataModel personPII2 = new PersonPIIDataModel();
            EmployeeDataModel employee1 = new EmployeeDataModel();
            employee1.setPersonPII(personPII1);
            EmployeeDataModel employee2 = new EmployeeDataModel();
            employee2.setPersonPII(personPII2);
            GetEmployeeResponseDTO dto1 = new GetEmployeeResponseDTO();
            GetEmployeeResponseDTO dto2 = new GetEmployeeResponseDTO();

            when(employeeRepository.findAll()).thenReturn(List.of(employee1, employee2));
            when(modelMapper.map(personPII1, GetEmployeeResponseDTO.class)).thenReturn(dto1);
            doNothing().when(modelMapper).map(employee1, dto1);
            when(modelMapper.map(personPII2, GetEmployeeResponseDTO.class)).thenReturn(dto2);
            doNothing().when(modelMapper).map(employee2, dto2);

            // When
            List<GetEmployeeResponseDTO> result = useCase.getAll();

            // Then
            assertThat(result).containsExactly(dto1, dto2);
            verify(employeeRepository, times(1)).findAll();
            verify(modelMapper, times(1)).map(personPII1, GetEmployeeResponseDTO.class);
            verify(modelMapper, times(1)).map(employee1, dto1);
            verify(modelMapper, times(1)).map(personPII2, GetEmployeeResponseDTO.class);
            verify(modelMapper, times(1)).map(employee2, dto2);
            verifyNoMoreInteractions(employeeRepository, modelMapper);
        }
    }
}
