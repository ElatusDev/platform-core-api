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

import com.akademiaplus.billing.membership.MembershipAdultStudentDataModel;
import com.akademiaplus.infra.persistence.config.TenantContextHolder;
import com.akademiaplus.membership.interfaceadapters.MembershipAdultStudentRepository;
import com.akademiaplus.utilities.EntityType;
import com.akademiaplus.utilities.exceptions.EntityNotFoundException;
import java.util.Optional;
import openapi.akademiaplus.domain.billing.dto.GetMembershipAdultStudentResponseDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;

@DisplayName("GetMembershipAdultStudentByIdUseCase")
@ExtendWith(MockitoExtension.class)
class GetMembershipAdultStudentByIdUseCaseTest {

    private static final Long TENANT_ID = 1L;
    private static final Long MEMBERSHIP_ADULT_STUDENT_ID = 100L;

    @Mock private MembershipAdultStudentRepository membershipAdultStudentRepository;
    @Mock private TenantContextHolder tenantContextHolder;
    @Mock private ModelMapper modelMapper;

    private GetMembershipAdultStudentByIdUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new GetMembershipAdultStudentByIdUseCase(membershipAdultStudentRepository, tenantContextHolder, modelMapper);
    }

    @Nested
    @DisplayName("Retrieval")
    class Retrieval {
        @Test
        @DisplayName("Should return mapped DTO when entity found")
        void shouldReturnMappedDto_whenMembershipAdultStudentFound() {
            // Given
            MembershipAdultStudentDataModel membershipAdultStudent = new MembershipAdultStudentDataModel();
            GetMembershipAdultStudentResponseDTO expectedDto = new GetMembershipAdultStudentResponseDTO();
            when(tenantContextHolder.getTenantId()).thenReturn(Optional.of(TENANT_ID));
            when(membershipAdultStudentRepository.findById(new MembershipAdultStudentDataModel.MembershipAdultStudentCompositeId(TENANT_ID, MEMBERSHIP_ADULT_STUDENT_ID)))
                    .thenReturn(Optional.of(membershipAdultStudent));
            when(modelMapper.map(membershipAdultStudent, GetMembershipAdultStudentResponseDTO.class)).thenReturn(expectedDto);
            // When
            GetMembershipAdultStudentResponseDTO result = useCase.get(MEMBERSHIP_ADULT_STUDENT_ID);
            // Then
            assertThat(result).isEqualTo(expectedDto);
            verify(tenantContextHolder).getTenantId();
            verify(membershipAdultStudentRepository).findById(new MembershipAdultStudentDataModel.MembershipAdultStudentCompositeId(TENANT_ID, MEMBERSHIP_ADULT_STUDENT_ID));
            verify(modelMapper).map(membershipAdultStudent, GetMembershipAdultStudentResponseDTO.class);
            verifyNoMoreInteractions(tenantContextHolder, membershipAdultStudentRepository, modelMapper);
        }
    }

    @Nested
    @DisplayName("Not found")
    class NotFound {
        @Test
        @DisplayName("Should throw EntityNotFoundException when entity not found")
        void shouldThrowEntityNotFoundException_whenMembershipAdultStudentNotFound() {
            // Given
            when(tenantContextHolder.getTenantId()).thenReturn(Optional.of(TENANT_ID));
            when(membershipAdultStudentRepository.findById(new MembershipAdultStudentDataModel.MembershipAdultStudentCompositeId(TENANT_ID, MEMBERSHIP_ADULT_STUDENT_ID)))
                    .thenReturn(Optional.empty());
            // When & Then
            assertThatThrownBy(() -> useCase.get(MEMBERSHIP_ADULT_STUDENT_ID))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasFieldOrPropertyWithValue("entityType", EntityType.MEMBERSHIP_ADULT_STUDENT)
                    .hasFieldOrPropertyWithValue("entityId", String.valueOf(MEMBERSHIP_ADULT_STUDENT_ID));
            verify(tenantContextHolder).getTenantId();
            verify(membershipAdultStudentRepository).findById(new MembershipAdultStudentDataModel.MembershipAdultStudentCompositeId(TENANT_ID, MEMBERSHIP_ADULT_STUDENT_ID));
            verifyNoMoreInteractions(tenantContextHolder, membershipAdultStudentRepository, modelMapper);
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
            assertThatThrownBy(() -> useCase.get(MEMBERSHIP_ADULT_STUDENT_ID))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage(GetMembershipAdultStudentByIdUseCase.ERROR_TENANT_CONTEXT_REQUIRED);
            verify(tenantContextHolder).getTenantId();
            verifyNoMoreInteractions(tenantContextHolder, membershipAdultStudentRepository, modelMapper);
        }
    }
}
