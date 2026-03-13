/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.notification.usecases;

import com.akademiaplus.infra.persistence.config.TenantContextHolder;
import com.akademiaplus.newsfeed.NewsFeedItemDataModel;
import com.akademiaplus.newsfeed.NewsFeedStatus;
import com.akademiaplus.notification.interfaceadapters.NewsFeedItemRepository;
import com.akademiaplus.utilities.EntityType;
import com.akademiaplus.utilities.exceptions.EntityNotFoundException;
import openapi.akademiaplus.domain.notification.system.dto.NewsFeedItemCreationRequestDTO;
import openapi.akademiaplus.domain.notification.system.dto.NewsFeedItemCreationResponseDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@DisplayName("UpdateNewsFeedItemUseCase")
@ExtendWith(MockitoExtension.class)
class UpdateNewsFeedItemUseCaseTest {

    private static final Long TENANT_ID = 1L;
    private static final Long NEWS_FEED_ITEM_ID = 100L;
    private static final String TITLE = "Updated Title";
    private static final String BODY = "Updated body content";
    private static final Long AUTHOR_ID = 1L;

    @Mock private NewsFeedItemRepository newsFeedItemRepository;
    @Mock private TenantContextHolder tenantContextHolder;
    @Mock private ModelMapper modelMapper;

    private UpdateNewsFeedItemUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new UpdateNewsFeedItemUseCase(newsFeedItemRepository, tenantContextHolder, modelMapper);
    }

    private NewsFeedItemCreationRequestDTO buildDto() {
        NewsFeedItemCreationRequestDTO dto = new NewsFeedItemCreationRequestDTO();
        dto.setTitle(TITLE);
        dto.setBody(BODY);
        dto.setAuthorId(AUTHOR_ID);
        return dto;
    }

    @Nested
    @DisplayName("Successful Update")
    class SuccessfulUpdate {

        @Test
        @DisplayName("Should find existing item and map DTO onto it")
        void shouldFindAndMapDto_whenItemExists() {
            // Given
            NewsFeedItemCreationRequestDTO dto = buildDto();
            NewsFeedItemDataModel existing = new NewsFeedItemDataModel();
            NewsFeedItemDataModel saved = new NewsFeedItemDataModel();
            NewsFeedItemCreationResponseDTO expectedResponse = new NewsFeedItemCreationResponseDTO();
            expectedResponse.setNewsFeedItemId(NEWS_FEED_ITEM_ID);

            NewsFeedItemDataModel.NewsFeedItemCompositeId compositeId =
                    new NewsFeedItemDataModel.NewsFeedItemCompositeId(TENANT_ID, NEWS_FEED_ITEM_ID);

            when(tenantContextHolder.requireTenantId()).thenReturn(TENANT_ID);
            when(newsFeedItemRepository.findById(compositeId)).thenReturn(Optional.of(existing));
            doNothing().when(modelMapper).map(dto, existing, UpdateNewsFeedItemUseCase.MAP_NAME);
            when(newsFeedItemRepository.saveAndFlush(existing)).thenReturn(saved);
            when(modelMapper.map(saved, NewsFeedItemCreationResponseDTO.class))
                    .thenReturn(expectedResponse);

            // When
            NewsFeedItemCreationResponseDTO result = useCase.update(NEWS_FEED_ITEM_ID, dto);

            // Then
            assertThat(result.getNewsFeedItemId()).isEqualTo(NEWS_FEED_ITEM_ID);
            InOrder inOrder = inOrder(tenantContextHolder, newsFeedItemRepository, modelMapper);
            inOrder.verify(tenantContextHolder, times(1)).requireTenantId();
            inOrder.verify(newsFeedItemRepository, times(1)).findById(compositeId);
            inOrder.verify(modelMapper, times(1)).map(dto, existing, UpdateNewsFeedItemUseCase.MAP_NAME);
            inOrder.verify(newsFeedItemRepository, times(1)).saveAndFlush(existing);
            inOrder.verify(modelMapper, times(1)).map(saved, NewsFeedItemCreationResponseDTO.class);
            inOrder.verifyNoMoreInteractions();
        }

        @Test
        @DisplayName("Should set publishedAt when status transitions to PUBLISHED and publishedAt is null")
        void shouldSetPublishedAt_whenStatusTransitionsToPublished() {
            // Given
            NewsFeedItemCreationRequestDTO dto = buildDto();
            dto.setStatus(NewsFeedItemCreationRequestDTO.StatusEnum.PUBLISHED);

            NewsFeedItemDataModel existing = new NewsFeedItemDataModel();
            existing.setStatus(NewsFeedStatus.DRAFT);
            existing.setPublishedAt(null);

            NewsFeedItemDataModel saved = new NewsFeedItemDataModel();
            NewsFeedItemCreationResponseDTO expectedResponse = new NewsFeedItemCreationResponseDTO();

            NewsFeedItemDataModel.NewsFeedItemCompositeId compositeId =
                    new NewsFeedItemDataModel.NewsFeedItemCompositeId(TENANT_ID, NEWS_FEED_ITEM_ID);

            when(tenantContextHolder.requireTenantId()).thenReturn(TENANT_ID);
            when(newsFeedItemRepository.findById(compositeId)).thenReturn(Optional.of(existing));
            doNothing().when(modelMapper).map(dto, existing, UpdateNewsFeedItemUseCase.MAP_NAME);
            when(newsFeedItemRepository.saveAndFlush(existing)).thenReturn(saved);
            when(modelMapper.map(saved, NewsFeedItemCreationResponseDTO.class))
                    .thenReturn(expectedResponse);

            // When
            useCase.update(NEWS_FEED_ITEM_ID, dto);

            // Then
            assertThat(existing.getStatus()).isEqualTo(NewsFeedStatus.PUBLISHED);
            assertThat(existing.getPublishedAt()).isNotNull();
        }

        @Test
        @DisplayName("Should not overwrite publishedAt when already set")
        void shouldNotOverwritePublishedAt_whenAlreadySet() {
            // Given
            NewsFeedItemCreationRequestDTO dto = buildDto();
            dto.setStatus(NewsFeedItemCreationRequestDTO.StatusEnum.PUBLISHED);

            LocalDateTime originalPublishedAt = LocalDateTime.of(2026, 1, 15, 10, 0);
            NewsFeedItemDataModel existing = new NewsFeedItemDataModel();
            existing.setStatus(NewsFeedStatus.PUBLISHED);
            existing.setPublishedAt(originalPublishedAt);

            NewsFeedItemDataModel saved = new NewsFeedItemDataModel();
            NewsFeedItemCreationResponseDTO expectedResponse = new NewsFeedItemCreationResponseDTO();

            NewsFeedItemDataModel.NewsFeedItemCompositeId compositeId =
                    new NewsFeedItemDataModel.NewsFeedItemCompositeId(TENANT_ID, NEWS_FEED_ITEM_ID);

            when(tenantContextHolder.requireTenantId()).thenReturn(TENANT_ID);
            when(newsFeedItemRepository.findById(compositeId)).thenReturn(Optional.of(existing));
            doNothing().when(modelMapper).map(dto, existing, UpdateNewsFeedItemUseCase.MAP_NAME);
            when(newsFeedItemRepository.saveAndFlush(existing)).thenReturn(saved);
            when(modelMapper.map(saved, NewsFeedItemCreationResponseDTO.class))
                    .thenReturn(expectedResponse);

            // When
            useCase.update(NEWS_FEED_ITEM_ID, dto);

            // Then
            assertThat(existing.getPublishedAt()).isEqualTo(originalPublishedAt);
        }

        @Test
        @DisplayName("Should not set publishedAt when status is not PUBLISHED")
        void shouldNotSetPublishedAt_whenStatusIsNotPublished() {
            // Given
            NewsFeedItemCreationRequestDTO dto = buildDto();
            dto.setStatus(NewsFeedItemCreationRequestDTO.StatusEnum.DRAFT);

            NewsFeedItemDataModel existing = new NewsFeedItemDataModel();
            existing.setStatus(NewsFeedStatus.DRAFT);
            existing.setPublishedAt(null);

            NewsFeedItemDataModel saved = new NewsFeedItemDataModel();
            NewsFeedItemCreationResponseDTO expectedResponse = new NewsFeedItemCreationResponseDTO();

            NewsFeedItemDataModel.NewsFeedItemCompositeId compositeId =
                    new NewsFeedItemDataModel.NewsFeedItemCompositeId(TENANT_ID, NEWS_FEED_ITEM_ID);

            when(tenantContextHolder.requireTenantId()).thenReturn(TENANT_ID);
            when(newsFeedItemRepository.findById(compositeId)).thenReturn(Optional.of(existing));
            doNothing().when(modelMapper).map(dto, existing, UpdateNewsFeedItemUseCase.MAP_NAME);
            when(newsFeedItemRepository.saveAndFlush(existing)).thenReturn(saved);
            when(modelMapper.map(saved, NewsFeedItemCreationResponseDTO.class))
                    .thenReturn(expectedResponse);

            // When
            useCase.update(NEWS_FEED_ITEM_ID, dto);

            // Then
            assertThat(existing.getStatus()).isEqualTo(NewsFeedStatus.DRAFT);
            assertThat(existing.getPublishedAt()).isNull();
        }
    }

    @Nested
    @DisplayName("Item Not Found")
    class ItemNotFound {

        @Test
        @DisplayName("Should throw EntityNotFoundException when item does not exist")
        void shouldThrowEntityNotFound_whenItemDoesNotExist() {
            // Given
            NewsFeedItemCreationRequestDTO dto = buildDto();
            NewsFeedItemDataModel.NewsFeedItemCompositeId compositeId =
                    new NewsFeedItemDataModel.NewsFeedItemCompositeId(TENANT_ID, NEWS_FEED_ITEM_ID);

            when(tenantContextHolder.requireTenantId()).thenReturn(TENANT_ID);
            when(newsFeedItemRepository.findById(compositeId)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> useCase.update(NEWS_FEED_ITEM_ID, dto))
                    .isInstanceOf(EntityNotFoundException.class)
                    .satisfies(ex -> {
                        EntityNotFoundException enfe = (EntityNotFoundException) ex;
                        assertThat(enfe.getEntityType()).isEqualTo(EntityType.NEWS_FEED_ITEM);
                        assertThat(enfe.getEntityId()).isEqualTo(String.valueOf(NEWS_FEED_ITEM_ID));
                    });

            InOrder inOrder = inOrder(tenantContextHolder, newsFeedItemRepository);
            inOrder.verify(tenantContextHolder, times(1)).requireTenantId();
            inOrder.verify(newsFeedItemRepository, times(1)).findById(compositeId);
            inOrder.verifyNoMoreInteractions();
            verifyNoInteractions(modelMapper);
        }
    }
}
