/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.notification.usecases;

import com.akademiaplus.newsfeed.NewsFeedItemDataModel;
import com.akademiaplus.notification.interfaceadapters.NewsFeedItemRepository;
import openapi.akademiaplus.domain.notification.system.dto.GetNewsFeedItemResponseDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@DisplayName("GetAllNewsFeedItemsUseCase")
@ExtendWith(MockitoExtension.class)
class GetAllNewsFeedItemsUseCaseTest {

    private static final Long COURSE_ID = 5L;

    @Mock private NewsFeedItemRepository newsFeedItemRepository;
    @Mock private ModelMapper modelMapper;

    private GetAllNewsFeedItemsUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new GetAllNewsFeedItemsUseCase(newsFeedItemRepository, modelMapper);
    }

    @Nested
    @DisplayName("Retrieval")
    class Retrieval {

        @Test
        @DisplayName("Should return empty list when no published items exist")
        void shouldReturnEmptyList_whenNoPublishedItemsExist() {
            // Given
            when(newsFeedItemRepository.findPublished(null)).thenReturn(Collections.emptyList());

            // When
            List<GetNewsFeedItemResponseDTO> result = useCase.getAll(null);

            // Then
            assertThat(result).isEmpty();
            verify(newsFeedItemRepository, times(1)).findPublished(null);
            verifyNoMoreInteractions(newsFeedItemRepository, modelMapper);
        }

        @Test
        @DisplayName("Should return mapped DTOs when published items exist")
        void shouldReturnMappedDtos_whenPublishedItemsExist() {
            // Given
            NewsFeedItemDataModel item1 = new NewsFeedItemDataModel();
            NewsFeedItemDataModel item2 = new NewsFeedItemDataModel();
            GetNewsFeedItemResponseDTO dto1 = new GetNewsFeedItemResponseDTO();
            GetNewsFeedItemResponseDTO dto2 = new GetNewsFeedItemResponseDTO();

            when(newsFeedItemRepository.findPublished(null)).thenReturn(List.of(item1, item2));
            when(modelMapper.map(item1, GetNewsFeedItemResponseDTO.class)).thenReturn(dto1);
            when(modelMapper.map(item2, GetNewsFeedItemResponseDTO.class)).thenReturn(dto2);

            // When
            List<GetNewsFeedItemResponseDTO> result = useCase.getAll(null);

            // Then
            assertThat(result).containsExactly(dto1, dto2);
            InOrder inOrder = inOrder(newsFeedItemRepository, modelMapper);
            inOrder.verify(newsFeedItemRepository, times(1)).findPublished(null);
            inOrder.verify(modelMapper, times(1)).map(item1, GetNewsFeedItemResponseDTO.class);
            inOrder.verify(modelMapper, times(1)).map(item2, GetNewsFeedItemResponseDTO.class);
            inOrder.verifyNoMoreInteractions();
        }

        @Test
        @DisplayName("Should pass courseId filter to repository")
        void shouldPassCourseIdFilter_whenCourseIdProvided() {
            // Given
            NewsFeedItemDataModel item = new NewsFeedItemDataModel();
            GetNewsFeedItemResponseDTO dto = new GetNewsFeedItemResponseDTO();

            when(newsFeedItemRepository.findPublished(COURSE_ID)).thenReturn(List.of(item));
            when(modelMapper.map(item, GetNewsFeedItemResponseDTO.class)).thenReturn(dto);

            // When
            List<GetNewsFeedItemResponseDTO> result = useCase.getAll(COURSE_ID);

            // Then
            assertThat(result).containsExactly(dto);
            verify(newsFeedItemRepository, times(1)).findPublished(COURSE_ID);
        }
    }
}
