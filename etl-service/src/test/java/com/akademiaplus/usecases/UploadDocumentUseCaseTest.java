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
import com.akademiaplus.domain.MigrationStatus;
import com.akademiaplus.interfaceadapters.MigrationJobRepository;
import com.akademiaplus.interfaceadapters.MigrationRowRepository;
import com.akademiaplus.util.ExcelParser;
import com.akademiaplus.util.ParsedSheet;
import com.akademiaplus.util.WordParser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;
import org.springframework.mock.web.MockMultipartFile;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("UploadDocumentUseCase Tests")
class UploadDocumentUseCaseTest {

    private static final Long TENANT_ID = 1L;
    private static final String CREATED_BY = "test-user";
    private static final String FILE_NAME = "students.xlsx";
    private static final byte[] FILE_CONTENT = new byte[]{1, 2, 3};

    @Mock
    private MigrationJobRepository jobRepository;
    @Mock
    private MigrationRowRepository rowRepository;
    @Mock
    private ExcelParser excelParser;
    @Mock
    private WordParser wordParser;

    @InjectMocks
    private UploadDocumentUseCase useCase;

    @Nested
    @DisplayName("Successful Upload")
    class SuccessfulUpload {

        @Test
        @DisplayName("Should create PARSED job with rows when given valid Excel file")
        void shouldCreateParsedJobWithRows_whenGivenValidExcelFile() {
            // Given
            MockMultipartFile file = new MockMultipartFile("file", FILE_NAME,
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", FILE_CONTENT);

            List<Map<String, String>> rows = List.of(
                    Map.of("Name", "John Doe", "Email", "john@test.com"),
                    Map.of("Name", "Jane Doe", "Email", "jane@test.com")
            );
            ParsedSheet sheet = new ParsedSheet("Sheet1", List.of("Name", "Email"), rows);
            when(excelParser.parse(isA(InputStream.class), eq(FILE_NAME))).thenReturn(List.of(sheet));
            when(jobRepository.findByTenantIdAndSourceFileNameAndStatusNot(TENANT_ID, FILE_NAME, MigrationStatus.FAILED))
                    .thenReturn(Optional.empty());

            when(jobRepository.save(isA(MigrationJob.class))).thenAnswer((Answer<MigrationJob>) inv -> {
                MigrationJob j = inv.getArgument(0);
                j.setId("job-123");
                return j;
            });

            // When
            MigrationJob result = useCase.execute(file, MigrationEntityType.ADULT_STUDENT, TENANT_ID, CREATED_BY);

            // Then
            assertThat(result.getStatus()).isEqualTo(MigrationStatus.PARSED);
            assertThat(result.getTenantId()).isEqualTo(TENANT_ID);
            assertThat(result.getSourceFileName()).isEqualTo(FILE_NAME);
            assertThat(result.getTotalRows()).isEqualTo(2);
            assertThat(result.getEntityType()).isEqualTo(MigrationEntityType.ADULT_STUDENT);

            ArgumentCaptor<InputStream> streamCaptor = ArgumentCaptor.forClass(InputStream.class);
            InOrder inOrder = inOrder(jobRepository, rowRepository, excelParser);
            inOrder.verify(jobRepository, times(1)).findByTenantIdAndSourceFileNameAndStatusNot(TENANT_ID, FILE_NAME, MigrationStatus.FAILED);
            inOrder.verify(excelParser, times(1)).parse(streamCaptor.capture(), eq(FILE_NAME));
            assertThat(streamCaptor.getValue()).isNotNull();
            inOrder.verify(jobRepository, times(1)).save(isA(MigrationJob.class));
            inOrder.verify(rowRepository, times(1)).saveAll(isA(List.class));
            inOrder.verify(jobRepository, times(1)).save(isA(MigrationJob.class));
            inOrder.verifyNoMoreInteractions();
            verifyNoInteractions(wordParser);
        }
    }

    @Nested
    @DisplayName("Upload Failures")
    class UploadFailures {

        @Test
        @DisplayName("Should throw exception when given empty file")
        void shouldThrowException_whenGivenEmptyFile() {
            // Given
            MockMultipartFile file = new MockMultipartFile("file", FILE_NAME,
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", new byte[0]);

            // When & Then
            assertThatThrownBy(() -> useCase.execute(file, null, TENANT_ID, CREATED_BY))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage(UploadDocumentUseCase.ERROR_FILE_EMPTY);

            verifyNoInteractions(jobRepository, rowRepository, excelParser, wordParser);
        }

        @Test
        @DisplayName("Should throw exception when given unsupported file format")
        void shouldThrowException_whenGivenUnsupportedFormat() {
            // Given
            MockMultipartFile file = new MockMultipartFile("file", "data.csv",
                    "text/csv", FILE_CONTENT);

            // When & Then
            assertThatThrownBy(() -> useCase.execute(file, null, TENANT_ID, CREATED_BY))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage(UploadDocumentUseCase.ERROR_UNSUPPORTED_FORMAT);

            verifyNoInteractions(jobRepository, rowRepository, excelParser, wordParser);
        }

        @Test
        @DisplayName("Should throw exception when duplicate active job exists for tenant")
        void shouldThrowException_whenDuplicateActiveJobExists() {
            // Given
            MockMultipartFile file = new MockMultipartFile("file", FILE_NAME,
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", FILE_CONTENT);

            MigrationJob existingJob = MigrationJob.builder()
                    .id("existing-job")
                    .tenantId(TENANT_ID)
                    .sourceFileName(FILE_NAME)
                    .status(MigrationStatus.PARSED)
                    .build();
            when(jobRepository.findByTenantIdAndSourceFileNameAndStatusNot(TENANT_ID, FILE_NAME, MigrationStatus.FAILED))
                    .thenReturn(Optional.of(existingJob));

            // When & Then
            assertThatThrownBy(() -> useCase.execute(file, null, TENANT_ID, CREATED_BY))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("already exists");

            verify(jobRepository, times(1)).findByTenantIdAndSourceFileNameAndStatusNot(TENANT_ID, FILE_NAME, MigrationStatus.FAILED);
            verifyNoMoreInteractions(jobRepository);
            verifyNoInteractions(rowRepository, excelParser, wordParser);
        }
    }
}
