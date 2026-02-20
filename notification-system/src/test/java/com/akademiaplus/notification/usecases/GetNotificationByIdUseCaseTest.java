/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.notification.usecases;

import com.akademiaplus.infra.persistence.config.TenantContextHolder;
import com.akademiaplus.notification.interfaceadapters.NotificationRepository;
import com.akademiaplus.notifications.NotificationDataModel;
import com.akademiaplus.utilities.EntityType;
import com.akademiaplus.utilities.exceptions.EntityNotFoundException;
import openapi.akademiaplus.domain.notification.system.dto.GetNotificationResponseDTO;
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

@DisplayName("GetNotificationByIdUseCase")
@ExtendWith(MockitoExtension.class)
class GetNotificationByIdUseCaseTest {

    private static final Long TENANT_ID = 1L;
    private static final Long NOTIFICATION_ID = 100L;

    @Mock private NotificationRepository notificationRepository;
    @Mock private TenantContextHolder tenantContextHolder;
    @Mock private ModelMapper modelMapper;

    private GetNotificationByIdUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new GetNotificationByIdUseCase(notificationRepository, tenantContextHolder, modelMapper);
    }

    @Nested
    @DisplayName("Retrieval")
    class Retrieval {

        @Test
        @DisplayName("Should return mapped DTO when notification is found")
        void shouldReturnMappedDto_whenNotificationFound() {
            // Given
            NotificationDataModel notification = new NotificationDataModel();
            GetNotificationResponseDTO expectedDto = new GetNotificationResponseDTO();

            when(tenantContextHolder.getTenantId()).thenReturn(Optional.of(TENANT_ID));
            when(notificationRepository.findById(
                    new NotificationDataModel.NotificationCompositeId(TENANT_ID, NOTIFICATION_ID)))
                    .thenReturn(Optional.of(notification));
            when(modelMapper.map(notification, GetNotificationResponseDTO.class)).thenReturn(expectedDto);

            // When
            GetNotificationResponseDTO result = useCase.get(NOTIFICATION_ID);

            // Then
            assertThat(result).isEqualTo(expectedDto);
            verify(tenantContextHolder).getTenantId();
            verify(notificationRepository).findById(
                    new NotificationDataModel.NotificationCompositeId(TENANT_ID, NOTIFICATION_ID));
            verify(modelMapper).map(notification, GetNotificationResponseDTO.class);
            verifyNoMoreInteractions(tenantContextHolder, notificationRepository, modelMapper);
        }
    }

    @Nested
    @DisplayName("Not found")
    class NotFound {

        @Test
        @DisplayName("Should throw EntityNotFoundException when notification not found")
        void shouldThrowEntityNotFoundException_whenNotificationNotFound() {
            // Given
            when(tenantContextHolder.getTenantId()).thenReturn(Optional.of(TENANT_ID));
            when(notificationRepository.findById(
                    new NotificationDataModel.NotificationCompositeId(TENANT_ID, NOTIFICATION_ID)))
                    .thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> useCase.get(NOTIFICATION_ID))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasFieldOrPropertyWithValue("entityType", EntityType.NOTIFICATION)
                    .hasFieldOrPropertyWithValue("entityId", String.valueOf(NOTIFICATION_ID));
            verify(tenantContextHolder).getTenantId();
            verify(notificationRepository).findById(
                    new NotificationDataModel.NotificationCompositeId(TENANT_ID, NOTIFICATION_ID));
            verifyNoMoreInteractions(tenantContextHolder, notificationRepository, modelMapper);
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
            assertThatThrownBy(() -> useCase.get(NOTIFICATION_ID))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage(GetNotificationByIdUseCase.ERROR_TENANT_CONTEXT_REQUIRED);
            verify(tenantContextHolder).getTenantId();
            verifyNoMoreInteractions(tenantContextHolder, notificationRepository, modelMapper);
        }
    }
}
