/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.membership.usecases;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.akademiaplus.billing.membership.MembershipDataModel;
import com.akademiaplus.exception.MembershipNotFoundException;
import com.akademiaplus.infra.persistence.config.TenantContextHolder;
import com.akademiaplus.membership.interfaceadapters.MembershipRepository;
import java.util.Optional;
import openapi.akademiaplus.domain.billing.dto.GetMembershipResponseDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;

@DisplayName("GetMembershipByIdUseCase")
@ExtendWith(MockitoExtension.class)
class GetMembershipByIdUseCaseTest {

    private static final Long TENANT_ID = 1L;
    private static final Long MEMBERSHIP_ID = 100L;

    @Mock private MembershipRepository membershipRepository;
    @Mock private TenantContextHolder tenantContextHolder;
    @Mock private ModelMapper modelMapper;

    private GetMembershipByIdUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new GetMembershipByIdUseCase(membershipRepository, tenantContextHolder, modelMapper);
    }

    @Nested
    @DisplayName("Retrieval")
    class Retrieval {
        @Test
        @DisplayName("Should return mapped DTO when entity found")
        void shouldReturnMappedDto_whenMembershipFound() {
            // Given
            MembershipDataModel membership = new MembershipDataModel();
            GetMembershipResponseDTO expectedDto = new GetMembershipResponseDTO();
            when(tenantContextHolder.getTenantId()).thenReturn(Optional.of(TENANT_ID));
            when(membershipRepository.findById(new MembershipDataModel.MembershipCompositeId(TENANT_ID, MEMBERSHIP_ID)))
                    .thenReturn(Optional.of(membership));
            when(modelMapper.map(membership, GetMembershipResponseDTO.class)).thenReturn(expectedDto);
            // When
            GetMembershipResponseDTO result = useCase.get(MEMBERSHIP_ID);
            // Then
            assertThat(result).isEqualTo(expectedDto);
            verify(tenantContextHolder).getTenantId();
            verify(membershipRepository).findById(new MembershipDataModel.MembershipCompositeId(TENANT_ID, MEMBERSHIP_ID));
            verify(modelMapper).map(membership, GetMembershipResponseDTO.class);
            verifyNoMoreInteractions(tenantContextHolder, membershipRepository, modelMapper);
        }
    }

    @Nested
    @DisplayName("Not found")
    class NotFound {
        @Test
        @DisplayName("Should throw NotFoundException when entity not found")
        void shouldThrowMembershipNotFoundException_whenMembershipNotFound() {
            // Given
            when(tenantContextHolder.getTenantId()).thenReturn(Optional.of(TENANT_ID));
            when(membershipRepository.findById(new MembershipDataModel.MembershipCompositeId(TENANT_ID, MEMBERSHIP_ID)))
                    .thenReturn(Optional.empty());
            // When & Then
            assertThatThrownBy(() -> useCase.get(MEMBERSHIP_ID))
                    .isInstanceOf(MembershipNotFoundException.class)
                    .hasMessage(String.valueOf(MEMBERSHIP_ID));
            verify(tenantContextHolder).getTenantId();
            verify(membershipRepository).findById(new MembershipDataModel.MembershipCompositeId(TENANT_ID, MEMBERSHIP_ID));
            verifyNoMoreInteractions(tenantContextHolder, membershipRepository, modelMapper);
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
            assertThatThrownBy(() -> useCase.get(MEMBERSHIP_ID))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage(GetMembershipByIdUseCase.ERROR_TENANT_CONTEXT_REQUIRED);
            verify(tenantContextHolder).getTenantId();
            verifyNoMoreInteractions(tenantContextHolder, membershipRepository, modelMapper);
        }
    }
}
