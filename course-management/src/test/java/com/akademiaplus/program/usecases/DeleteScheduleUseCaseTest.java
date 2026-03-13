/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.program.usecases;

import com.akademiaplus.courses.program.ScheduleDataModel;
import com.akademiaplus.infra.persistence.config.TenantContextHolder;
import com.akademiaplus.program.interfaceadapters.ScheduleRepository;
import com.akademiaplus.utilities.EntityType;
import com.akademiaplus.utilities.exceptions.EntityDeletionNotAllowedException;
import com.akademiaplus.utilities.exceptions.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link DeleteScheduleUseCase}.
 *
 * @author ElatusDev
 * @since 1.0
 */
@DisplayName("DeleteScheduleUseCase")
@ExtendWith(MockitoExtension.class)
class DeleteScheduleUseCaseTest {

    private static final Long TENANT_ID = 1L;
    private static final Long SCHEDULE_ID = 200L;

    @Mock
    private ScheduleRepository repository;

    @Mock
    private TenantContextHolder tenantContextHolder;

    private DeleteScheduleUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new DeleteScheduleUseCase(repository, tenantContextHolder);
    }

    @Nested
    @DisplayName("Successful Deletion")
    class SuccessfulDeletion {

        @Test
        @DisplayName("Should soft-delete schedule when found by composite key")
        void shouldSoftDeleteSchedule_whenFoundByCompositeKey() {
            // Given
            when(tenantContextHolder.requireTenantId()).thenReturn(TENANT_ID);
            ScheduleDataModel entity = new ScheduleDataModel();
            ScheduleDataModel.ScheduleCompositeId compositeId =
                    new ScheduleDataModel.ScheduleCompositeId(TENANT_ID, SCHEDULE_ID);
            when(repository.findById(compositeId)).thenReturn(Optional.of(entity));

            // When
            useCase.delete(SCHEDULE_ID);

            // Then
            InOrder inOrder = inOrder(tenantContextHolder, repository);
            inOrder.verify(tenantContextHolder, times(1)).requireTenantId();
            inOrder.verify(repository, times(1)).findById(compositeId);
            inOrder.verify(repository, times(1)).delete(entity);
            inOrder.verifyNoMoreInteractions();
        }
    }

    @Nested
    @DisplayName("Entity Not Found")
    class EntityNotFound {

        @Test
        @DisplayName("Should throw EntityNotFoundException when schedule missing")
        void shouldThrowEntityNotFound_whenScheduleMissing() {
            // Given
            when(tenantContextHolder.requireTenantId()).thenReturn(TENANT_ID);
            ScheduleDataModel.ScheduleCompositeId compositeId =
                    new ScheduleDataModel.ScheduleCompositeId(TENANT_ID, SCHEDULE_ID);
            when(repository.findById(compositeId)).thenReturn(Optional.empty());

            // When / Then
            assertThatThrownBy(() -> useCase.delete(SCHEDULE_ID))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessage(String.format(EntityNotFoundException.MESSAGE_TEMPLATE,
                            EntityType.SCHEDULE, String.valueOf(SCHEDULE_ID)))
                    .satisfies(ex -> {
                        EntityNotFoundException enfe = (EntityNotFoundException) ex;
                        assertThat(enfe.getEntityType()).isEqualTo(EntityType.SCHEDULE);
                        assertThat(enfe.getEntityId()).isEqualTo(String.valueOf(SCHEDULE_ID));
                    });

            verify(tenantContextHolder, times(1)).requireTenantId();
            verify(repository, times(1)).findById(compositeId);
            verifyNoMoreInteractions(tenantContextHolder, repository);
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
            ScheduleDataModel entity = new ScheduleDataModel();
            ScheduleDataModel.ScheduleCompositeId compositeId =
                    new ScheduleDataModel.ScheduleCompositeId(TENANT_ID, SCHEDULE_ID);
            when(repository.findById(compositeId)).thenReturn(Optional.of(entity));
            doThrow(new DataIntegrityViolationException("FK constraint"))
                    .when(repository).delete(entity);

            // When / Then
            assertThatThrownBy(() -> useCase.delete(SCHEDULE_ID))
                    .isInstanceOf(EntityDeletionNotAllowedException.class)
                    .hasMessage(String.format(EntityDeletionNotAllowedException.MESSAGE_TEMPLATE,
                            EntityType.SCHEDULE, String.valueOf(SCHEDULE_ID)))
                    .satisfies(ex -> {
                        EntityDeletionNotAllowedException edna =
                                (EntityDeletionNotAllowedException) ex;
                        assertThat(edna.getEntityType()).isEqualTo(EntityType.SCHEDULE);
                        assertThat(edna.getEntityId()).isEqualTo(String.valueOf(SCHEDULE_ID));
                        assertThat(edna.getReason()).isNull();
                    });

            verify(tenantContextHolder, times(1)).requireTenantId();
            verify(repository, times(1)).findById(compositeId);
            verify(repository, times(1)).delete(entity);
            verifyNoMoreInteractions(tenantContextHolder, repository);
        }
    }
}
