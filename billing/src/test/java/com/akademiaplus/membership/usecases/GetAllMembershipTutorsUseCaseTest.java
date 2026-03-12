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

import com.akademiaplus.billing.membership.MembershipTutorDataModel;
import com.akademiaplus.membership.interfaceadapters.MembershipTutorRepository;
import java.util.Collections;
import java.util.List;
import openapi.akademiaplus.domain.billing.dto.GetMembershipTutorResponseDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;

@DisplayName("GetAllMembershipTutorsUseCase")
@ExtendWith(MockitoExtension.class)
class GetAllMembershipTutorsUseCaseTest {

    @Mock private MembershipTutorRepository membershipTutorRepository;
    @Mock private ModelMapper modelMapper;

    private GetAllMembershipTutorsUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new GetAllMembershipTutorsUseCase(membershipTutorRepository, modelMapper);
    }

    @Nested
    @DisplayName("Retrieval")
    class Retrieval {

        @Test
        @DisplayName("Should return empty list when no membership tutors exist")
        void shouldReturnEmptyList_whenNoMembershipTutorsExist() {
            // Given
            when(membershipTutorRepository.findAll()).thenReturn(Collections.emptyList());
            // When
            List<GetMembershipTutorResponseDTO> result = useCase.getAll();
            // Then
            assertThat(result).isEmpty();
            verify(membershipTutorRepository, times(1)).findAll();
            verifyNoMoreInteractions(membershipTutorRepository, modelMapper);
        }

        @Test
        @DisplayName("Should return mapped DTOs when membership tutors exist")
        void shouldReturnMappedDtos_whenMembershipTutorsExist() {
            // Given
            MembershipTutorDataModel membershipTutor1 = new MembershipTutorDataModel();
            MembershipTutorDataModel membershipTutor2 = new MembershipTutorDataModel();
            GetMembershipTutorResponseDTO dto1 = new GetMembershipTutorResponseDTO();
            GetMembershipTutorResponseDTO dto2 = new GetMembershipTutorResponseDTO();
            when(membershipTutorRepository.findAll()).thenReturn(List.of(membershipTutor1, membershipTutor2));
            when(modelMapper.map(membershipTutor1, GetMembershipTutorResponseDTO.class)).thenReturn(dto1);
            when(modelMapper.map(membershipTutor2, GetMembershipTutorResponseDTO.class)).thenReturn(dto2);
            // When
            List<GetMembershipTutorResponseDTO> result = useCase.getAll();
            // Then
            assertThat(result).containsExactly(dto1, dto2);
            verify(membershipTutorRepository, times(1)).findAll();
            verify(modelMapper, times(1)).map(membershipTutor1, GetMembershipTutorResponseDTO.class);
            verify(modelMapper, times(1)).map(membershipTutor2, GetMembershipTutorResponseDTO.class);
            verifyNoMoreInteractions(membershipTutorRepository, modelMapper);
        }
    }
}
