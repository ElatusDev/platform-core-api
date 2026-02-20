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
 * Unit tests for {@link DeleteNotificationUseCase}.
 *
 * @author ElatusDev
 * @since 1.0
 */
@DisplayName("DeleteNotificationUseCase")
@ExtendWith(MockitoExtension.class)
class DeleteNotificationUseCaseTest {

    private static final Long TENANT_ID = 1L;
    private static final Long NOTIFICATION_ID = 100L;

    @Mock
    private NotificationRepository repository;

    @Mock
    private TenantContextHolder tenantContextHolder;

    private DeleteNotificationUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new DeleteNotificationUseCase(repository, tenantContextHolder);
    }

    @Nested
    @DisplayName("Successful Deletion")
    class SuccessfulDeletion {

        @Test
        @DisplayName("Should soft-delete notification when found by composite key")
        void shouldSoftDeleteNotification_whenFoundByCompositeKey() {
            // Given
            when(tenantContextHolder.requireTenantId()).thenReturn(TENANT_ID);
            NotificationDataModel entity = new NotificationDataModel();
            NotificationDataModel.NotificationCompositeId compositeId =
                    new NotificationDataModel.NotificationCompositeId(TENANT_ID, NOTIFICATION_ID);
            when(repository.findById(compositeId)).thenReturn(Optional.of(entity));

            // When
            useCase.delete(NOTIFICATION_ID);

            // Then
            verify(repository).delete(entity);
        }
    }

    @Nested
    @DisplayName("Entity Not Found")
    class EntityNotFound {

        @Test
        @DisplayName("Should throw EntityNotFoundException when notification missing")
        void shouldThrowEntityNotFound_whenNotificationMissing() {
            // Given
            when(tenantContextHolder.requireTenantId()).thenReturn(TENANT_ID);
            NotificationDataModel.NotificationCompositeId compositeId =
                    new NotificationDataModel.NotificationCompositeId(TENANT_ID, NOTIFICATION_ID);
            when(repository.findById(compositeId)).thenReturn(Optional.empty());

            // When / Then
            assertThatThrownBy(() -> useCase.delete(NOTIFICATION_ID))
                    .isInstanceOf(EntityNotFoundException.class)
                    .satisfies(ex -> {
                        EntityNotFoundException enfe = (EntityNotFoundException) ex;
                        assertThat(enfe.getEntityType()).isEqualTo(EntityType.NOTIFICATION);
                        assertThat(enfe.getEntityId()).isEqualTo(String.valueOf(NOTIFICATION_ID));
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
            NotificationDataModel entity = new NotificationDataModel();
            NotificationDataModel.NotificationCompositeId compositeId =
                    new NotificationDataModel.NotificationCompositeId(TENANT_ID, NOTIFICATION_ID);
            when(repository.findById(compositeId)).thenReturn(Optional.of(entity));
            doThrow(new DataIntegrityViolationException("FK constraint"))
                    .when(repository).delete(entity);

            // When / Then
            assertThatThrownBy(() -> useCase.delete(NOTIFICATION_ID))
                    .isInstanceOf(EntityDeletionNotAllowedException.class)
                    .satisfies(ex -> {
                        EntityDeletionNotAllowedException edna =
                                (EntityDeletionNotAllowedException) ex;
                        assertThat(edna.getEntityType()).isEqualTo(EntityType.NOTIFICATION);
                        assertThat(edna.getEntityId()).isEqualTo(String.valueOf(NOTIFICATION_ID));
                        assertThat(edna.getReason()).isNull();
                    });
        }
    }
}
