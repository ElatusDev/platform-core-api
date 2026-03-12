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
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.akademiaplus.billing.membership.MembershipTutorDataModel;
import com.akademiaplus.infra.persistence.config.TenantContextHolder;
import com.akademiaplus.membership.interfaceadapters.MembershipTutorRepository;
import com.akademiaplus.utilities.EntityType;
import com.akademiaplus.utilities.exceptions.EntityNotFoundException;
import java.util.Optional;
import openapi.akademiaplus.domain.billing.dto.GetMembershipTutorResponseDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;

@DisplayName("GetMembershipTutorByIdUseCase")
@ExtendWith(MockitoExtension.class)
class GetMembershipTutorByIdUseCaseTest {

    private static final Long TENANT_ID = 1L;
    private static final Long MEMBERSHIP_TUTOR_ID = 100L;

    @Mock private MembershipTutorRepository membershipTutorRepository;
    @Mock private TenantContextHolder tenantContextHolder;
    @Mock private ModelMapper modelMapper;

    private GetMembershipTutorByIdUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new GetMembershipTutorByIdUseCase(membershipTutorRepository, tenantContextHolder, modelMapper);
    }

    @Nested
    @DisplayName("Retrieval")
    class Retrieval {
        @Test
        @DisplayName("Should return mapped DTO when entity found")
        void shouldReturnMappedDto_whenMembershipTutorFound() {
            // Given
            MembershipTutorDataModel membershipTutor = new MembershipTutorDataModel();
            GetMembershipTutorResponseDTO expectedDto = new GetMembershipTutorResponseDTO();
            when(tenantContextHolder.getTenantId()).thenReturn(Optional.of(TENANT_ID));
            when(membershipTutorRepository.findById(new MembershipTutorDataModel.MembershipTutorCompositeId(TENANT_ID, MEMBERSHIP_TUTOR_ID)))
                    .thenReturn(Optional.of(membershipTutor));
            when(modelMapper.map(membershipTutor, GetMembershipTutorResponseDTO.class)).thenReturn(expectedDto);
            // When
            GetMembershipTutorResponseDTO result = useCase.get(MEMBERSHIP_TUTOR_ID);
            // Then
            assertThat(result).isEqualTo(expectedDto);
            verify(tenantContextHolder, times(1)).getTenantId();
            verify(membershipTutorRepository, times(1)).findById(new MembershipTutorDataModel.MembershipTutorCompositeId(TENANT_ID, MEMBERSHIP_TUTOR_ID));
            verify(modelMapper, times(1)).map(membershipTutor, GetMembershipTutorResponseDTO.class);
            verifyNoMoreInteractions(tenantContextHolder, membershipTutorRepository, modelMapper);
        }
    }

    @Nested
    @DisplayName("Not found")
    class NotFound {
        @Test
        @DisplayName("Should throw NotFoundException when entity not found")
        void shouldThrowEntityNotFoundException_whenMembershipTutorNotFound() {
            // Given
            when(tenantContextHolder.getTenantId()).thenReturn(Optional.of(TENANT_ID));
            when(membershipTutorRepository.findById(new MembershipTutorDataModel.MembershipTutorCompositeId(TENANT_ID, MEMBERSHIP_TUTOR_ID)))
                    .thenReturn(Optional.empty());
            // When & Then
            assertThatThrownBy(() -> useCase.get(MEMBERSHIP_TUTOR_ID))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasFieldOrPropertyWithValue("entityType", EntityType.MEMBERSHIP_TUTOR)
                    .hasFieldOrPropertyWithValue("entityId", String.valueOf(MEMBERSHIP_TUTOR_ID));
            verify(tenantContextHolder, times(1)).getTenantId();
            verify(membershipTutorRepository, times(1)).findById(new MembershipTutorDataModel.MembershipTutorCompositeId(TENANT_ID, MEMBERSHIP_TUTOR_ID));
            verifyNoMoreInteractions(tenantContextHolder, membershipTutorRepository, modelMapper);
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
            assertThatThrownBy(() -> useCase.get(MEMBERSHIP_TUTOR_ID))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage(GetMembershipTutorByIdUseCase.ERROR_TENANT_CONTEXT_REQUIRED);
            verify(tenantContextHolder, times(1)).getTenantId();
            verifyNoMoreInteractions(tenantContextHolder, membershipTutorRepository, modelMapper);
        }
    }
}
