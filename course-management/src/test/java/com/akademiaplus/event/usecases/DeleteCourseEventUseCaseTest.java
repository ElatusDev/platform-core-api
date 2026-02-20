/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.event.usecases;

import com.akademiaplus.courses.event.CourseEventDataModel;
import com.akademiaplus.event.interfaceadapters.CourseEventRepository;
import com.akademiaplus.infra.persistence.config.TenantContextHolder;
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
 * Unit tests for {@link DeleteCourseEventUseCase}.
 *
 * @author ElatusDev
 * @since 1.0
 */
@DisplayName("DeleteCourseEventUseCase")
@ExtendWith(MockitoExtension.class)
class DeleteCourseEventUseCaseTest {

    private static final Long TENANT_ID = 1L;
    private static final Long COURSE_EVENT_ID = 300L;

    @Mock
    private CourseEventRepository repository;

    @Mock
    private TenantContextHolder tenantContextHolder;

    private DeleteCourseEventUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new DeleteCourseEventUseCase(repository, tenantContextHolder);
    }

    @Nested
    @DisplayName("Successful Deletion")
    class SuccessfulDeletion {

        @Test
        @DisplayName("Should soft-delete course event when found by composite key")
        void shouldSoftDeleteCourseEvent_whenFoundByCompositeKey() {
            // Given
            when(tenantContextHolder.requireTenantId()).thenReturn(TENANT_ID);
            CourseEventDataModel entity = new CourseEventDataModel();
            CourseEventDataModel.CourseEventCompositeId compositeId =
                    new CourseEventDataModel.CourseEventCompositeId(TENANT_ID, COURSE_EVENT_ID);
            when(repository.findById(compositeId)).thenReturn(Optional.of(entity));

            // When
            useCase.delete(COURSE_EVENT_ID);

            // Then
            verify(repository).delete(entity);
        }
    }

    @Nested
    @DisplayName("Entity Not Found")
    class EntityNotFound {

        @Test
        @DisplayName("Should throw EntityNotFoundException when course event missing")
        void shouldThrowEntityNotFound_whenCourseEventMissing() {
            // Given
            when(tenantContextHolder.requireTenantId()).thenReturn(TENANT_ID);
            CourseEventDataModel.CourseEventCompositeId compositeId =
                    new CourseEventDataModel.CourseEventCompositeId(TENANT_ID, COURSE_EVENT_ID);
            when(repository.findById(compositeId)).thenReturn(Optional.empty());

            // When / Then
            assertThatThrownBy(() -> useCase.delete(COURSE_EVENT_ID))
                    .isInstanceOf(EntityNotFoundException.class)
                    .satisfies(ex -> {
                        EntityNotFoundException enfe = (EntityNotFoundException) ex;
                        assertThat(enfe.getEntityType()).isEqualTo(EntityType.COURSE_EVENT);
                        assertThat(enfe.getEntityId()).isEqualTo(String.valueOf(COURSE_EVENT_ID));
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
            CourseEventDataModel entity = new CourseEventDataModel();
            CourseEventDataModel.CourseEventCompositeId compositeId =
                    new CourseEventDataModel.CourseEventCompositeId(TENANT_ID, COURSE_EVENT_ID);
            when(repository.findById(compositeId)).thenReturn(Optional.of(entity));
            doThrow(new DataIntegrityViolationException("FK constraint"))
                    .when(repository).delete(entity);

            // When / Then
            assertThatThrownBy(() -> useCase.delete(COURSE_EVENT_ID))
                    .isInstanceOf(EntityDeletionNotAllowedException.class)
                    .satisfies(ex -> {
                        EntityDeletionNotAllowedException edna =
                                (EntityDeletionNotAllowedException) ex;
                        assertThat(edna.getEntityType()).isEqualTo(EntityType.COURSE_EVENT);
                        assertThat(edna.getEntityId()).isEqualTo(String.valueOf(COURSE_EVENT_ID));
                        assertThat(edna.getReason()).isNull();
                    });
        }
    }
}
