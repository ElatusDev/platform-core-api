/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.membership.usecases;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.akademiaplus.billing.membership.MembershipAdultStudentDataModel;
import com.akademiaplus.membership.interfaceadapters.MembershipAdultStudentRepository;
import java.util.Collections;
import java.util.List;
import openapi.akademiaplus.domain.billing.dto.GetMembershipAdultStudentResponseDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;

@DisplayName("GetAllMembershipAdultStudentsUseCase")
@ExtendWith(MockitoExtension.class)
class GetAllMembershipAdultStudentsUseCaseTest {

    @Mock private MembershipAdultStudentRepository membershipAdultStudentRepository;
    @Mock private ModelMapper modelMapper;

    private GetAllMembershipAdultStudentsUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new GetAllMembershipAdultStudentsUseCase(membershipAdultStudentRepository, modelMapper);
    }

    @Nested
    @DisplayName("Retrieval")
    class Retrieval {

        @Test
        @DisplayName("Should return empty list when no membership adult students exist")
        void shouldReturnEmptyList_whenNoMembershipAdultStudentsExist() {
            // Given
            when(membershipAdultStudentRepository.findAll()).thenReturn(Collections.emptyList());
            // When
            List<GetMembershipAdultStudentResponseDTO> result = useCase.getAll();
            // Then
            assertThat(result).isEmpty();
            verify(membershipAdultStudentRepository, times(1)).findAll();
            verifyNoMoreInteractions(membershipAdultStudentRepository, modelMapper);
        }

        @Test
        @DisplayName("Should return mapped DTOs when membership adult students exist")
        void shouldReturnMappedDtos_whenMembershipAdultStudentsExist() {
            // Given
            MembershipAdultStudentDataModel membershipAdultStudent1 = new MembershipAdultStudentDataModel();
            MembershipAdultStudentDataModel membershipAdultStudent2 = new MembershipAdultStudentDataModel();
            GetMembershipAdultStudentResponseDTO dto1 = new GetMembershipAdultStudentResponseDTO();
            GetMembershipAdultStudentResponseDTO dto2 = new GetMembershipAdultStudentResponseDTO();
            when(membershipAdultStudentRepository.findAll()).thenReturn(List.of(membershipAdultStudent1, membershipAdultStudent2));
            when(modelMapper.map(membershipAdultStudent1, GetMembershipAdultStudentResponseDTO.class)).thenReturn(dto1);
            when(modelMapper.map(membershipAdultStudent2, GetMembershipAdultStudentResponseDTO.class)).thenReturn(dto2);
            // When
            List<GetMembershipAdultStudentResponseDTO> result = useCase.getAll();
            // Then
            assertThat(result).containsExactly(dto1, dto2);
            verify(membershipAdultStudentRepository, times(1)).findAll();
            verify(modelMapper, times(1)).map(membershipAdultStudent1, GetMembershipAdultStudentResponseDTO.class);
            verify(modelMapper, times(1)).map(membershipAdultStudent2, GetMembershipAdultStudentResponseDTO.class);
            verifyNoMoreInteractions(membershipAdultStudentRepository, modelMapper);
        }
    }
}
