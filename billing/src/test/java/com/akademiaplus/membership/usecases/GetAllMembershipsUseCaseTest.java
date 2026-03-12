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

import com.akademiaplus.billing.membership.MembershipDataModel;
import com.akademiaplus.membership.interfaceadapters.MembershipRepository;
import java.util.Collections;
import java.util.List;
import openapi.akademiaplus.domain.billing.dto.GetMembershipResponseDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;

@DisplayName("GetAllMembershipsUseCase")
@ExtendWith(MockitoExtension.class)
class GetAllMembershipsUseCaseTest {

    @Mock private MembershipRepository membershipRepository;
    @Mock private ModelMapper modelMapper;

    private GetAllMembershipsUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new GetAllMembershipsUseCase(membershipRepository, modelMapper);
    }

    @Nested
    @DisplayName("Retrieval")
    class Retrieval {

        @Test
        @DisplayName("Should return empty list when no memberships exist")
        void shouldReturnEmptyList_whenNoMembershipsExist() {
            // Given
            when(membershipRepository.findAll()).thenReturn(Collections.emptyList());
            // When
            List<GetMembershipResponseDTO> result = useCase.getAll();
            // Then
            assertThat(result).isEmpty();
            verify(membershipRepository, times(1)).findAll();
            verifyNoMoreInteractions(membershipRepository, modelMapper);
        }

        @Test
        @DisplayName("Should return mapped DTOs when memberships exist")
        void shouldReturnMappedDtos_whenMembershipsExist() {
            // Given
            MembershipDataModel membership1 = new MembershipDataModel();
            MembershipDataModel membership2 = new MembershipDataModel();
            GetMembershipResponseDTO dto1 = new GetMembershipResponseDTO();
            GetMembershipResponseDTO dto2 = new GetMembershipResponseDTO();
            when(membershipRepository.findAll()).thenReturn(List.of(membership1, membership2));
            when(modelMapper.map(membership1, GetMembershipResponseDTO.class)).thenReturn(dto1);
            when(modelMapper.map(membership2, GetMembershipResponseDTO.class)).thenReturn(dto2);
            // When
            List<GetMembershipResponseDTO> result = useCase.getAll();
            // Then
            assertThat(result).containsExactly(dto1, dto2);
            verify(membershipRepository, times(1)).findAll();
            verify(modelMapper, times(1)).map(membership1, GetMembershipResponseDTO.class);
            verify(modelMapper, times(1)).map(membership2, GetMembershipResponseDTO.class);
            verifyNoMoreInteractions(membershipRepository, modelMapper);
        }
    }
}
