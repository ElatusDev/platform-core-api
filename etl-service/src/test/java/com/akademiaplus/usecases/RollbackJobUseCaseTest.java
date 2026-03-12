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
import com.akademiaplus.domain.RollbackResult;
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

import java.time.Instant;
import java.util.List;
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
@DisplayName("RollbackJobUseCase Tests")
class RollbackJobUseCaseTest {

    private static final String JOB_ID = "job-123";
    private static final Long TENANT_ID = 1L;

    @Mock
    private MigrationJobRepository jobRepository;
    @Mock
    private MigrationRowRepository rowRepository;

    @InjectMocks
    private RollbackJobUseCase useCase;

    @Nested
    @DisplayName("Successful Rollback")
    class SuccessfulRollback {

        @Test
        @DisplayName("Should reset rows to VALID and job to VALIDATED when given completed job")
        void shouldResetRowsAndJob_whenGivenCompletedJob() {
            // Given
            MigrationJob job = MigrationJob.builder()
                    .id(JOB_ID)
                    .tenantId(TENANT_ID)
                    .entityType(MigrationEntityType.ADULT_STUDENT)
                    .status(MigrationStatus.COMPLETED)
                    .loadedRows(2)
                    .build();
            when(jobRepository.findById(JOB_ID)).thenReturn(Optional.of(job));

            MigrationRow row1 = new MigrationRow();
            row1.setJobId(JOB_ID);
            row1.setRowNumber(1);
            row1.setStatus(RowStatus.LOADED);
            row1.setTargetEntityId(100L);
            row1.setLoadedAt(Instant.now());

            MigrationRow row2 = new MigrationRow();
            row2.setJobId(JOB_ID);
            row2.setRowNumber(2);
            row2.setStatus(RowStatus.LOADED);
            row2.setTargetEntityId(101L);
            row2.setLoadedAt(Instant.now());

            when(rowRepository.findByJobIdAndStatus(JOB_ID, RowStatus.LOADED)).thenReturn(List.of(row1, row2));
            when(jobRepository.save(job)).thenReturn(job);

            // When
            RollbackResult result = useCase.execute(JOB_ID);

            // Then
            assertThat(result.jobId()).isEqualTo(JOB_ID);
            assertThat(result.rolledBack()).isEqualTo(2);
            assertThat(result.skipped()).isEqualTo(0);

            assertThat(job.getStatus()).isEqualTo(MigrationStatus.VALIDATED);
            assertThat(job.getLoadedRows()).isEqualTo(0);

            assertThat(row1.getStatus()).isEqualTo(RowStatus.VALID);
            assertThat(row1.getTargetEntityId()).isNull();
            assertThat(row1.getLoadedAt()).isNull();

            assertThat(row2.getStatus()).isEqualTo(RowStatus.VALID);
            assertThat(row2.getTargetEntityId()).isNull();
            assertThat(row2.getLoadedAt()).isNull();

            InOrder inOrder = inOrder(jobRepository, rowRepository);
            inOrder.verify(jobRepository, times(1)).findById(JOB_ID);
            inOrder.verify(rowRepository, times(1)).findByJobIdAndStatus(JOB_ID, RowStatus.LOADED);
            inOrder.verify(rowRepository, times(1)).saveAll(List.of(row1, row2));
            inOrder.verify(jobRepository, times(1)).save(job);
            inOrder.verifyNoMoreInteractions();
        }

        @Test
        @DisplayName("Should handle rows with null targetEntityId gracefully")
        void shouldHandleNullTargetEntityId_gracefully() {
            // Given
            MigrationJob job = MigrationJob.builder()
                    .id(JOB_ID)
                    .tenantId(TENANT_ID)
                    .entityType(MigrationEntityType.ADULT_STUDENT)
                    .status(MigrationStatus.COMPLETED)
                    .loadedRows(1)
                    .build();
            when(jobRepository.findById(JOB_ID)).thenReturn(Optional.of(job));

            MigrationRow row = new MigrationRow();
            row.setJobId(JOB_ID);
            row.setRowNumber(1);
            row.setStatus(RowStatus.LOADED);
            row.setTargetEntityId(null);

            when(rowRepository.findByJobIdAndStatus(JOB_ID, RowStatus.LOADED)).thenReturn(List.of(row));
            when(jobRepository.save(job)).thenReturn(job);

            // When
            RollbackResult result = useCase.execute(JOB_ID);

            // Then
            assertThat(result.rolledBack()).isEqualTo(1);
            assertThat(row.getStatus()).isEqualTo(RowStatus.VALID);

            InOrder inOrder = inOrder(jobRepository, rowRepository);
            inOrder.verify(jobRepository, times(1)).findById(JOB_ID);
            inOrder.verify(rowRepository, times(1)).findByJobIdAndStatus(JOB_ID, RowStatus.LOADED);
            inOrder.verify(rowRepository, times(1)).saveAll(List.of(row));
            inOrder.verify(jobRepository, times(1)).save(job);
            inOrder.verifyNoMoreInteractions();
        }
    }

    @Nested
    @DisplayName("Rollback Failures")
    class RollbackFailures {

        @Test
        @DisplayName("Should throw exception when job not found")
        void shouldThrowException_whenJobNotFound() {
            // Given
            when(jobRepository.findById(JOB_ID)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> useCase.execute(JOB_ID))
                    .isInstanceOf(NoSuchElementException.class)
                    .hasMessage(String.format(RollbackJobUseCase.ERROR_JOB_NOT_FOUND, JOB_ID));

            verify(jobRepository, times(1)).findById(JOB_ID);
            verifyNoMoreInteractions(jobRepository);
            verifyNoInteractions(rowRepository);
        }

        @Test
        @DisplayName("Should throw exception when job is not in COMPLETED status")
        void shouldThrowException_whenJobNotCompleted() {
            // Given
            MigrationJob job = MigrationJob.builder()
                    .id(JOB_ID)
                    .status(MigrationStatus.VALIDATED)
                    .build();
            when(jobRepository.findById(JOB_ID)).thenReturn(Optional.of(job));

            // When & Then
            assertThatThrownBy(() -> useCase.execute(JOB_ID))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage(String.format(RollbackJobUseCase.ERROR_INVALID_STATUS, MigrationStatus.VALIDATED));

            verify(jobRepository, times(1)).findById(JOB_ID);
            verifyNoMoreInteractions(jobRepository);
            verifyNoInteractions(rowRepository);
        }
    }
}
