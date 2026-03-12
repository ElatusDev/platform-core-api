/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.usecases;

import com.akademiaplus.domain.MigrationEntityType;
import com.akademiaplus.domain.MigrationJob;
import com.akademiaplus.domain.MigrationRow;
import com.akademiaplus.domain.MigrationStatus;
import com.akademiaplus.domain.RowStatus;
import com.akademiaplus.interfaceadapters.MigrationJobRepository;
import com.akademiaplus.interfaceadapters.MigrationRowRepository;
import com.akademiaplus.util.EntityFieldDefinition;
import com.akademiaplus.util.EntityFieldRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ValidateJobUseCase Tests")
class ValidateJobUseCaseTest {

    private static final String JOB_ID = "job-123";
    private static final Long TENANT_ID = 1L;

    @Mock
    private MigrationJobRepository jobRepository;
    @Mock
    private MigrationRowRepository rowRepository;
    @Mock
    private EntityFieldRegistry fieldRegistry;

    @InjectMocks
    private ValidateJobUseCase useCase;

    @Nested
    @DisplayName("Successful Validation")
    class SuccessfulValidation {

        @Test
        @DisplayName("Should mark rows as VALID when all required fields present and correctly typed")
        void shouldMarkRowsAsValid_whenAllFieldsCorrect() {
            // Given
            MigrationJob job = MigrationJob.builder()
                    .id(JOB_ID)
                    .tenantId(TENANT_ID)
                    .entityType(MigrationEntityType.ADULT_STUDENT)
                    .status(MigrationStatus.MAPPING)
                    .build();
            when(jobRepository.findById(JOB_ID)).thenReturn(Optional.of(job));

            List<EntityFieldDefinition> fields = List.of(
                    new EntityFieldDefinition("firstName", "String", true, "First name"),
                    new EntityFieldDefinition("email", "String", false, "Email address")
            );
            when(fieldRegistry.getFields(MigrationEntityType.ADULT_STUDENT)).thenReturn(fields);

            MigrationRow row = new MigrationRow();
            row.setJobId(JOB_ID);
            row.setRowNumber(1);
            row.setStatus(RowStatus.MAPPED);
            row.setMappedData(Map.of("firstName", "John", "email", "john@test.com"));

            when(rowRepository.findByJobIdAndStatus(JOB_ID, RowStatus.MAPPED)).thenReturn(List.of(row));

            when(jobRepository.save(job)).thenReturn(job);

            // When
            MigrationJob result = useCase.execute(JOB_ID);

            // Then
            assertThat(result.getStatus()).isEqualTo(MigrationStatus.VALIDATED);
            assertThat(result.getValidRows()).isEqualTo(1);
            assertThat(result.getErrorRows()).isEqualTo(0);
            assertThat(row.getStatus()).isEqualTo(RowStatus.VALID);

            InOrder inOrder = inOrder(jobRepository, fieldRegistry, rowRepository);
            inOrder.verify(jobRepository, times(1)).findById(JOB_ID);
            inOrder.verify(fieldRegistry, times(1)).getFields(MigrationEntityType.ADULT_STUDENT);
            inOrder.verify(rowRepository, times(1)).findByJobIdAndStatus(JOB_ID, RowStatus.MAPPED);
            inOrder.verify(rowRepository, times(1)).saveAll(List.of(row));
            inOrder.verify(jobRepository, times(1)).save(job);
            inOrder.verifyNoMoreInteractions();
        }

        @Test
        @DisplayName("Should mark rows as INVALID when required field is missing")
        void shouldMarkRowsAsInvalid_whenRequiredFieldMissing() {
            // Given
            MigrationJob job = MigrationJob.builder()
                    .id(JOB_ID)
                    .tenantId(TENANT_ID)
                    .entityType(MigrationEntityType.ADULT_STUDENT)
                    .status(MigrationStatus.MAPPING)
                    .build();
            when(jobRepository.findById(JOB_ID)).thenReturn(Optional.of(job));

            List<EntityFieldDefinition> fields = List.of(
                    new EntityFieldDefinition("firstName", "String", true, "First name"),
                    new EntityFieldDefinition("lastName", "String", true, "Last name")
            );
            when(fieldRegistry.getFields(MigrationEntityType.ADULT_STUDENT)).thenReturn(fields);

            MigrationRow row = new MigrationRow();
            row.setJobId(JOB_ID);
            row.setRowNumber(1);
            row.setStatus(RowStatus.MAPPED);
            row.setMappedData(Map.of("firstName", "John"));

            when(rowRepository.findByJobIdAndStatus(JOB_ID, RowStatus.MAPPED)).thenReturn(List.of(row));
            when(jobRepository.save(job)).thenReturn(job);

            // When
            MigrationJob result = useCase.execute(JOB_ID);

            // Then
            assertThat(result.getErrorRows()).isEqualTo(1);
            assertThat(row.getStatus()).isEqualTo(RowStatus.INVALID);
            assertThat(row.getValidationErrors()).hasSize(1);
            assertThat(row.getValidationErrors().getFirst().field()).isEqualTo("lastName");

            InOrder inOrder = inOrder(jobRepository, fieldRegistry, rowRepository);
            inOrder.verify(jobRepository, times(1)).findById(JOB_ID);
            inOrder.verify(fieldRegistry, times(1)).getFields(MigrationEntityType.ADULT_STUDENT);
            inOrder.verify(rowRepository, times(1)).findByJobIdAndStatus(JOB_ID, RowStatus.MAPPED);
            inOrder.verify(rowRepository, times(1)).saveAll(List.of(row));
            inOrder.verify(jobRepository, times(1)).save(job);
            inOrder.verifyNoMoreInteractions();
        }
    }

    @Nested
    @DisplayName("Validation Failures")
    class ValidationFailures {

        @Test
        @DisplayName("Should throw exception when job not found")
        void shouldThrowException_whenJobNotFound() {
            // Given
            when(jobRepository.findById(JOB_ID)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> useCase.execute(JOB_ID))
                    .isInstanceOf(NoSuchElementException.class)
                    .hasMessage(String.format(ValidateJobUseCase.ERROR_JOB_NOT_FOUND, JOB_ID));

            verify(jobRepository, times(1)).findById(JOB_ID);
            verifyNoMoreInteractions(jobRepository);
            verifyNoInteractions(rowRepository, fieldRegistry);
        }

        @Test
        @DisplayName("Should throw exception when entity type is not set")
        void shouldThrowException_whenEntityTypeNotSet() {
            // Given
            MigrationJob job = MigrationJob.builder()
                    .id(JOB_ID)
                    .status(MigrationStatus.MAPPING)
                    .entityType(null)
                    .build();
            when(jobRepository.findById(JOB_ID)).thenReturn(Optional.of(job));

            // When & Then
            assertThatThrownBy(() -> useCase.execute(JOB_ID))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage(ValidateJobUseCase.ERROR_NO_ENTITY_TYPE);

            verify(jobRepository, times(1)).findById(JOB_ID);
            verifyNoMoreInteractions(jobRepository);
            verifyNoInteractions(rowRepository, fieldRegistry);
        }

        @Test
        @DisplayName("Should mark row INVALID when date field has wrong format")
        void shouldMarkRowInvalid_whenDateFieldHasWrongFormat() {
            // Given
            MigrationJob job = MigrationJob.builder()
                    .id(JOB_ID)
                    .tenantId(TENANT_ID)
                    .entityType(MigrationEntityType.ADULT_STUDENT)
                    .status(MigrationStatus.MAPPING)
                    .build();
            when(jobRepository.findById(JOB_ID)).thenReturn(Optional.of(job));

            List<EntityFieldDefinition> fields = List.of(
                    new EntityFieldDefinition("birthDate", "LocalDate", false, "Birth date")
            );
            when(fieldRegistry.getFields(MigrationEntityType.ADULT_STUDENT)).thenReturn(fields);

            MigrationRow row = new MigrationRow();
            row.setJobId(JOB_ID);
            row.setRowNumber(1);
            row.setStatus(RowStatus.MAPPED);
            row.setMappedData(Map.of("birthDate", "not-a-date"));

            when(rowRepository.findByJobIdAndStatus(JOB_ID, RowStatus.MAPPED)).thenReturn(List.of(row));
            when(jobRepository.save(job)).thenReturn(job);

            // When
            useCase.execute(JOB_ID);

            // Then
            assertThat(row.getStatus()).isEqualTo(RowStatus.INVALID);
            assertThat(row.getValidationErrors()).hasSize(1);
            assertThat(row.getValidationErrors().getFirst().message()).contains("Invalid date");

            InOrder inOrder = inOrder(jobRepository, fieldRegistry, rowRepository);
            inOrder.verify(jobRepository, times(1)).findById(JOB_ID);
            inOrder.verify(fieldRegistry, times(1)).getFields(MigrationEntityType.ADULT_STUDENT);
            inOrder.verify(rowRepository, times(1)).findByJobIdAndStatus(JOB_ID, RowStatus.MAPPED);
            inOrder.verify(rowRepository, times(1)).saveAll(List.of(row));
            inOrder.verify(jobRepository, times(1)).save(job);
            inOrder.verifyNoMoreInteractions();
        }

        @Test
        @DisplayName("Should mark row INVALID when email has wrong format")
        void shouldMarkRowInvalid_whenEmailHasWrongFormat() {
            // Given
            MigrationJob job = MigrationJob.builder()
                    .id(JOB_ID)
                    .tenantId(TENANT_ID)
                    .entityType(MigrationEntityType.ADULT_STUDENT)
                    .status(MigrationStatus.MAPPING)
                    .build();
            when(jobRepository.findById(JOB_ID)).thenReturn(Optional.of(job));

            List<EntityFieldDefinition> fields = List.of(
                    new EntityFieldDefinition("email", "String", false, "Email")
            );
            when(fieldRegistry.getFields(MigrationEntityType.ADULT_STUDENT)).thenReturn(fields);

            MigrationRow row = new MigrationRow();
            row.setJobId(JOB_ID);
            row.setRowNumber(1);
            row.setStatus(RowStatus.MAPPED);
            row.setMappedData(Map.of("email", "not-an-email"));

            when(rowRepository.findByJobIdAndStatus(JOB_ID, RowStatus.MAPPED)).thenReturn(List.of(row));
            when(jobRepository.save(job)).thenReturn(job);

            // When
            useCase.execute(JOB_ID);

            // Then
            assertThat(row.getStatus()).isEqualTo(RowStatus.INVALID);
            assertThat(row.getValidationErrors()).hasSize(1);
            assertThat(row.getValidationErrors().getFirst().field()).isEqualTo("email");

            InOrder inOrder = inOrder(jobRepository, fieldRegistry, rowRepository);
            inOrder.verify(jobRepository, times(1)).findById(JOB_ID);
            inOrder.verify(fieldRegistry, times(1)).getFields(MigrationEntityType.ADULT_STUDENT);
            inOrder.verify(rowRepository, times(1)).findByJobIdAndStatus(JOB_ID, RowStatus.MAPPED);
            inOrder.verify(rowRepository, times(1)).saveAll(List.of(row));
            inOrder.verify(jobRepository, times(1)).save(job);
            inOrder.verifyNoMoreInteractions();
        }
    }
}
