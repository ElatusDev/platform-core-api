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
@DisplayName("LoadIntoMariaDbUseCase Tests")
class LoadIntoMariaDbUseCaseTest {

    private static final String JOB_ID = "job-123";
    private static final Long TENANT_ID = 1L;

    @Mock
    private MigrationJobRepository jobRepository;
    @Mock
    private MigrationRowRepository rowRepository;

    @InjectMocks
    private LoadIntoMariaDbUseCase useCase;

    @Nested
    @DisplayName("Successful Load")
    class SuccessfulLoad {

        @Test
        @DisplayName("Should mark rows as LOADED and job as COMPLETED when given valid rows")
        void shouldMarkRowsAsLoaded_whenGivenValidRows() {
            // Given
            MigrationJob job = MigrationJob.builder()
                    .id(JOB_ID)
                    .tenantId(TENANT_ID)
                    .entityType(MigrationEntityType.ADULT_STUDENT)
                    .status(MigrationStatus.VALIDATED)
                    .validRows(2)
                    .build();
            when(jobRepository.findById(JOB_ID)).thenReturn(Optional.of(job));

            MigrationRow row1 = new MigrationRow();
            row1.setJobId(JOB_ID);
            row1.setRowNumber(1);
            row1.setStatus(RowStatus.VALID);
            row1.setMappedData(Map.of("firstName", "John"));

            MigrationRow row2 = new MigrationRow();
            row2.setJobId(JOB_ID);
            row2.setRowNumber(2);
            row2.setStatus(RowStatus.VALID);
            row2.setMappedData(Map.of("firstName", "Jane"));

            when(rowRepository.findByJobIdAndStatus(JOB_ID, RowStatus.VALID)).thenReturn(List.of(row1, row2));
            when(jobRepository.save(job)).thenReturn(job);

            // When
            MigrationJob result = useCase.execute(JOB_ID);

            // Then
            assertThat(result.getStatus()).isEqualTo(MigrationStatus.COMPLETED);
            assertThat(result.getLoadedRows()).isEqualTo(2);
            assertThat(row1.getStatus()).isEqualTo(RowStatus.LOADED);
            assertThat(row2.getStatus()).isEqualTo(RowStatus.LOADED);

            InOrder inOrder = inOrder(jobRepository, rowRepository);
            inOrder.verify(jobRepository, times(1)).findById(JOB_ID);
            inOrder.verify(jobRepository, times(1)).save(job);
            inOrder.verify(rowRepository, times(1)).findByJobIdAndStatus(JOB_ID, RowStatus.VALID);
            inOrder.verify(rowRepository, times(1)).saveAll(List.of(row1, row2));
            inOrder.verify(jobRepository, times(1)).save(job);
            inOrder.verifyNoMoreInteractions();
        }
    }

    @Nested
    @DisplayName("Load Failures")
    class LoadFailures {

        @Test
        @DisplayName("Should throw exception when job not found")
        void shouldThrowException_whenJobNotFound() {
            // Given
            when(jobRepository.findById(JOB_ID)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> useCase.execute(JOB_ID))
                    .isInstanceOf(NoSuchElementException.class)
                    .hasMessage(String.format(LoadIntoMariaDbUseCase.ERROR_JOB_NOT_FOUND, JOB_ID));

            verify(jobRepository, times(1)).findById(JOB_ID);
            verifyNoMoreInteractions(jobRepository);
            verifyNoInteractions(rowRepository);
        }

        @Test
        @DisplayName("Should throw exception when job is not in VALIDATED status")
        void shouldThrowException_whenJobNotValidated() {
            // Given
            MigrationJob job = MigrationJob.builder()
                    .id(JOB_ID)
                    .status(MigrationStatus.PARSED)
                    .entityType(MigrationEntityType.ADULT_STUDENT)
                    .validRows(1)
                    .build();
            when(jobRepository.findById(JOB_ID)).thenReturn(Optional.of(job));

            // When & Then
            assertThatThrownBy(() -> useCase.execute(JOB_ID))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage(String.format(LoadIntoMariaDbUseCase.ERROR_INVALID_STATUS, MigrationStatus.PARSED));

            verify(jobRepository, times(1)).findById(JOB_ID);
            verifyNoMoreInteractions(jobRepository);
            verifyNoInteractions(rowRepository);
        }

        @Test
        @DisplayName("Should throw exception when no valid rows exist")
        void shouldThrowException_whenNoValidRows() {
            // Given
            MigrationJob job = MigrationJob.builder()
                    .id(JOB_ID)
                    .status(MigrationStatus.VALIDATED)
                    .entityType(MigrationEntityType.ADULT_STUDENT)
                    .validRows(0)
                    .build();
            when(jobRepository.findById(JOB_ID)).thenReturn(Optional.of(job));

            // When & Then
            assertThatThrownBy(() -> useCase.execute(JOB_ID))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage(LoadIntoMariaDbUseCase.ERROR_NO_VALID_ROWS);

            verify(jobRepository, times(1)).findById(JOB_ID);
            verifyNoMoreInteractions(jobRepository);
            verifyNoInteractions(rowRepository);
        }

        @Test
        @DisplayName("Should throw exception when entity type is not set")
        void shouldThrowException_whenEntityTypeNotSet() {
            // Given
            MigrationJob job = MigrationJob.builder()
                    .id(JOB_ID)
                    .status(MigrationStatus.VALIDATED)
                    .entityType(null)
                    .validRows(1)
                    .build();
            when(jobRepository.findById(JOB_ID)).thenReturn(Optional.of(job));

            // When & Then
            assertThatThrownBy(() -> useCase.execute(JOB_ID))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage(LoadIntoMariaDbUseCase.ERROR_NO_ENTITY_TYPE);

            verify(jobRepository, times(1)).findById(JOB_ID);
            verifyNoMoreInteractions(jobRepository);
            verifyNoInteractions(rowRepository);
        }
    }
}
