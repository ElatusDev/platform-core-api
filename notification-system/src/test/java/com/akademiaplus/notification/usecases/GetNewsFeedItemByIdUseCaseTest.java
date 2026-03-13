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
import com.akademiaplus.notification.interfaceadapters.NewsFeedItemRepository;
import com.akademiaplus.utilities.EntityType;
import com.akademiaplus.utilities.exceptions.EntityNotFoundException;
import openapi.akademiaplus.domain.notification.system.dto.GetNewsFeedItemResponseDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@DisplayName("GetNewsFeedItemByIdUseCase")
@ExtendWith(MockitoExtension.class)
class GetNewsFeedItemByIdUseCaseTest {

    private static final Long TENANT_ID = 1L;
    private static final Long NEWS_FEED_ITEM_ID = 100L;

    @Mock private NewsFeedItemRepository newsFeedItemRepository;
    @Mock private TenantContextHolder tenantContextHolder;
    @Mock private ModelMapper modelMapper;

    private GetNewsFeedItemByIdUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new GetNewsFeedItemByIdUseCase(newsFeedItemRepository, tenantContextHolder, modelMapper);
    }

    @Nested
    @DisplayName("Retrieval")
    class Retrieval {

        @Test
        @DisplayName("Should return mapped DTO when news feed item is found")
        void shouldReturnMappedDto_whenNewsFeedItemFound() {
            // Given
            NewsFeedItemDataModel entity = new NewsFeedItemDataModel();
            GetNewsFeedItemResponseDTO expectedDto = new GetNewsFeedItemResponseDTO();

            when(tenantContextHolder.getTenantId()).thenReturn(Optional.of(TENANT_ID));
            when(newsFeedItemRepository.findById(
                    new NewsFeedItemDataModel.NewsFeedItemCompositeId(TENANT_ID, NEWS_FEED_ITEM_ID)))
                    .thenReturn(Optional.of(entity));
            when(modelMapper.map(entity, GetNewsFeedItemResponseDTO.class)).thenReturn(expectedDto);

            // When
            GetNewsFeedItemResponseDTO result = useCase.get(NEWS_FEED_ITEM_ID);

            // Then
            assertThat(result).isEqualTo(expectedDto);
            verify(tenantContextHolder, times(1)).getTenantId();
            verify(newsFeedItemRepository, times(1)).findById(
                    new NewsFeedItemDataModel.NewsFeedItemCompositeId(TENANT_ID, NEWS_FEED_ITEM_ID));
            verify(modelMapper, times(1)).map(entity, GetNewsFeedItemResponseDTO.class);
            verifyNoMoreInteractions(tenantContextHolder, newsFeedItemRepository, modelMapper);
        }
    }

    @Nested
    @DisplayName("Not found")
    class NotFound {

        @Test
        @DisplayName("Should throw EntityNotFoundException when news feed item not found")
        void shouldThrowEntityNotFoundException_whenNewsFeedItemNotFound() {
            // Given
            when(tenantContextHolder.getTenantId()).thenReturn(Optional.of(TENANT_ID));
            when(newsFeedItemRepository.findById(
                    new NewsFeedItemDataModel.NewsFeedItemCompositeId(TENANT_ID, NEWS_FEED_ITEM_ID)))
                    .thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> useCase.get(NEWS_FEED_ITEM_ID))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessage(String.format(EntityNotFoundException.MESSAGE_TEMPLATE,
                            EntityType.NEWS_FEED_ITEM, String.valueOf(NEWS_FEED_ITEM_ID)));

            // Rule 9 — verify downstream modelMapper was NOT called
            verify(tenantContextHolder, times(1)).getTenantId();
            verify(newsFeedItemRepository, times(1)).findById(
                    new NewsFeedItemDataModel.NewsFeedItemCompositeId(TENANT_ID, NEWS_FEED_ITEM_ID));
            verifyNoInteractions(modelMapper);
            verifyNoMoreInteractions(tenantContextHolder, newsFeedItemRepository);
        }
    }

    @Nested
    @DisplayName("Tenant context")
    class TenantContext {

        @Test
        @DisplayName("Should throw IllegalArgumentException when tenant context is missing")
        void shouldThrowIllegalArgumentException_whenTenantContextMissing() {
            // Given
            when(tenantContextHolder.getTenantId()).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> useCase.get(NEWS_FEED_ITEM_ID))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage(GetNewsFeedItemByIdUseCase.ERROR_TENANT_CONTEXT_REQUIRED);

            // Rule 9 — verify downstream mocks were NOT called
            verify(tenantContextHolder, times(1)).getTenantId();
            verifyNoInteractions(newsFeedItemRepository, modelMapper);
            verifyNoMoreInteractions(tenantContextHolder);
        }
    }
}
