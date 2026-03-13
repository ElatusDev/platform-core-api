/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.notification.usecases;

import com.akademiaplus.newsfeed.NewsFeedItemDataModel;
import com.akademiaplus.newsfeed.NewsFeedStatus;
import com.akademiaplus.notification.interfaceadapters.NewsFeedItemRepository;
import openapi.akademiaplus.domain.notification.system.dto.NewsFeedItemCreationRequestDTO;
import openapi.akademiaplus.domain.notification.system.dto.NewsFeedItemCreationResponseDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.springframework.context.ApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@DisplayName("NewsFeedItemCreationUseCase")
@ExtendWith(MockitoExtension.class)
class NewsFeedItemCreationUseCaseTest {

    @Mock private ApplicationContext applicationContext;
    @Mock private NewsFeedItemRepository newsFeedItemRepository;
    @Mock private ModelMapper modelMapper;

    private NewsFeedItemCreationUseCase useCase;

    private static final String TITLE = "School News";
    private static final String BODY = "Important announcement";
    private static final Long AUTHOR_ID = 1L;
    private static final Long SAVED_ID = 10L;

    @BeforeEach
    void setUp() {
        useCase = new NewsFeedItemCreationUseCase(
                applicationContext, newsFeedItemRepository, modelMapper);
    }

    private NewsFeedItemCreationRequestDTO buildDto() {
        NewsFeedItemCreationRequestDTO dto = new NewsFeedItemCreationRequestDTO();
        dto.setTitle(TITLE);
        dto.setBody(BODY);
        dto.setAuthorId(AUTHOR_ID);
        return dto;
    }

    @Nested
    @DisplayName("Transformation")
    class Transformation {

        @Test
        @DisplayName("Should default status to DRAFT when status is null")
        void shouldDefaultStatusToDraft_whenStatusIsNull() {
            // Given
            NewsFeedItemCreationRequestDTO dto = buildDto();
            NewsFeedItemDataModel prototypeModel = new NewsFeedItemDataModel();
            when(applicationContext.getBean(NewsFeedItemDataModel.class)).thenReturn(prototypeModel);

            // When
            NewsFeedItemDataModel result = useCase.transform(dto);

            // Then
            assertThat(result.getStatus()).isEqualTo(NewsFeedStatus.DRAFT);
            assertThat(result.getPublishedAt()).isNull();
        }

        @Test
        @DisplayName("Should set publishedAt when status is PUBLISHED")
        void shouldSetPublishedAt_whenStatusIsPublished() {
            // Given
            NewsFeedItemCreationRequestDTO dto = buildDto();
            dto.setStatus(NewsFeedItemCreationRequestDTO.StatusEnum.PUBLISHED);
            NewsFeedItemDataModel prototypeModel = new NewsFeedItemDataModel();
            when(applicationContext.getBean(NewsFeedItemDataModel.class)).thenReturn(prototypeModel);

            // When
            NewsFeedItemDataModel result = useCase.transform(dto);

            // Then
            assertThat(result.getStatus()).isEqualTo(NewsFeedStatus.PUBLISHED);
            assertThat(result.getPublishedAt()).isNotNull();
        }

        @Test
        @DisplayName("Should not set publishedAt when status is DRAFT")
        void shouldNotSetPublishedAt_whenStatusIsDraft() {
            // Given
            NewsFeedItemCreationRequestDTO dto = buildDto();
            dto.setStatus(NewsFeedItemCreationRequestDTO.StatusEnum.DRAFT);
            NewsFeedItemDataModel prototypeModel = new NewsFeedItemDataModel();
            when(applicationContext.getBean(NewsFeedItemDataModel.class)).thenReturn(prototypeModel);

            // When
            NewsFeedItemDataModel result = useCase.transform(dto);

            // Then
            assertThat(result.getStatus()).isEqualTo(NewsFeedStatus.DRAFT);
            assertThat(result.getPublishedAt()).isNull();
        }
    }

    @Nested
    @DisplayName("Persistence")
    class Persistence {

        @Test
        @DisplayName("Should save transformed model and return mapped DTO")
        void shouldSaveAndReturnDto_whenCreating() {
            // Given
            NewsFeedItemCreationRequestDTO dto = buildDto();
            NewsFeedItemDataModel prototypeModel = new NewsFeedItemDataModel();
            NewsFeedItemDataModel savedModel = new NewsFeedItemDataModel();
            savedModel.setNewsFeedItemId(SAVED_ID);
            NewsFeedItemCreationResponseDTO expectedDto = new NewsFeedItemCreationResponseDTO();
            expectedDto.setNewsFeedItemId(SAVED_ID);

            when(applicationContext.getBean(NewsFeedItemDataModel.class)).thenReturn(prototypeModel);
            doNothing().when(modelMapper).map(dto, prototypeModel, NewsFeedItemCreationUseCase.MAP_NAME);
            when(newsFeedItemRepository.saveAndFlush(prototypeModel)).thenReturn(savedModel);
            when(modelMapper.map(savedModel, NewsFeedItemCreationResponseDTO.class)).thenReturn(expectedDto);

            // When
            NewsFeedItemCreationResponseDTO result = useCase.create(dto);

            // Then
            verify(newsFeedItemRepository, times(1)).saveAndFlush(prototypeModel);
            assertThat(result.getNewsFeedItemId()).isEqualTo(SAVED_ID);
        }
    }
}
