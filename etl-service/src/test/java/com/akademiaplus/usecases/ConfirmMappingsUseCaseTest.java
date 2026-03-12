/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.usecases;

import com.akademiaplus.domain.ColumnMapping;
import com.akademiaplus.domain.MigrationEntityType;
import com.akademiaplus.domain.MigrationJob;
import com.akademiaplus.domain.MigrationStatus;
import com.akademiaplus.domain.TransformType;
import com.akademiaplus.interfaceadapters.MigrationJobRepository;
import com.akademiaplus.util.EntityFieldRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ConfirmMappingsUseCase Tests")
class ConfirmMappingsUseCaseTest {

    private static final String JOB_ID = "job-123";
    private static final Long TENANT_ID = 1L;

    @Mock
    private MigrationJobRepository jobRepository;
    @Mock
    private ApplyMappingUseCase applyMappingUseCase;
    @Mock
    private EntityFieldRegistry fieldRegistry;

    @InjectMocks
    private ConfirmMappingsUseCase useCase;

    @Nested
    @DisplayName("Validation Failures")
    class ValidationFailures {

        @Test
        @DisplayName("Should throw exception when job not found")
        void shouldThrowException_whenJobNotFound() {
            // Given
            when(jobRepository.findById(JOB_ID)).thenReturn(Optional.empty());
            List<ColumnMapping> mappings = List.of(
                    new ColumnMapping("Name", "firstName", TransformType.NONE));

            // When & Then
            assertThatThrownBy(() -> useCase.execute(JOB_ID, mappings))
                    .isInstanceOf(NoSuchElementException.class)
                    .hasMessage(String.format(ConfirmMappingsUseCase.ERROR_JOB_NOT_FOUND, JOB_ID));

            verify(jobRepository, times(1)).findById(JOB_ID);
            verifyNoMoreInteractions(jobRepository);
            verifyNoInteractions(fieldRegistry, applyMappingUseCase);
        }

        @Test
        @DisplayName("Should throw exception when job is in invalid status")
        void shouldThrowException_whenJobInInvalidStatus() {
            // Given
            MigrationJob job = MigrationJob.builder()
                    .id(JOB_ID)
                    .status(MigrationStatus.UPLOADED)
                    .build();
            when(jobRepository.findById(JOB_ID)).thenReturn(Optional.of(job));

            List<ColumnMapping> mappings = List.of(
                    new ColumnMapping("Name", "firstName", TransformType.NONE));

            // When & Then
            assertThatThrownBy(() -> useCase.execute(JOB_ID, mappings))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage(String.format(ConfirmMappingsUseCase.ERROR_INVALID_STATUS, MigrationStatus.UPLOADED));

            verify(jobRepository, times(1)).findById(JOB_ID);
            verifyNoMoreInteractions(jobRepository);
            verifyNoInteractions(fieldRegistry, applyMappingUseCase);
        }

        @Test
        @DisplayName("Should throw exception when required fields not covered by mappings")
        void shouldThrowException_whenRequiredFieldsNotCovered() {
            // Given
            MigrationJob job = MigrationJob.builder()
                    .id(JOB_ID)
                    .tenantId(TENANT_ID)
                    .entityType(MigrationEntityType.ADULT_STUDENT)
                    .status(MigrationStatus.ANALYZED)
                    .build();
            when(jobRepository.findById(JOB_ID)).thenReturn(Optional.of(job));
            when(fieldRegistry.getRequiredFieldNames(MigrationEntityType.ADULT_STUDENT))
                    .thenReturn(List.of("firstName", "lastName"));

            List<ColumnMapping> mappings = List.of(
                    new ColumnMapping("Name", "firstName", TransformType.NONE));

            // When & Then
            assertThatThrownBy(() -> useCase.execute(JOB_ID, mappings))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("lastName");

            verify(jobRepository, times(1)).findById(JOB_ID);
            verify(fieldRegistry, times(1)).getRequiredFieldNames(MigrationEntityType.ADULT_STUDENT);
            verifyNoMoreInteractions(jobRepository, fieldRegistry);
            verifyNoInteractions(applyMappingUseCase);
        }

        @Test
        @DisplayName("Should throw exception when entity type is not set")
        void shouldThrowException_whenEntityTypeNotSet() {
            // Given
            MigrationJob job = MigrationJob.builder()
                    .id(JOB_ID)
                    .entityType(null)
                    .status(MigrationStatus.ANALYZED)
                    .build();
            when(jobRepository.findById(JOB_ID)).thenReturn(Optional.of(job));

            List<ColumnMapping> mappings = List.of(
                    new ColumnMapping("Name", "firstName", TransformType.NONE));

            // When & Then
            assertThatThrownBy(() -> useCase.execute(JOB_ID, mappings))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage(ConfirmMappingsUseCase.ERROR_NO_ENTITY_TYPE);

            verify(jobRepository, times(1)).findById(JOB_ID);
            verifyNoMoreInteractions(jobRepository);
            verifyNoInteractions(fieldRegistry, applyMappingUseCase);
        }
    }
}
