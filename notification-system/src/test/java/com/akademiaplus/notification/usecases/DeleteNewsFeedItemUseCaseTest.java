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
import com.akademiaplus.utilities.exceptions.EntityDeletionNotAllowedException;
import com.akademiaplus.utilities.exceptions.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@DisplayName("DeleteNewsFeedItemUseCase")
@ExtendWith(MockitoExtension.class)
class DeleteNewsFeedItemUseCaseTest {

    private static final Long TENANT_ID = 1L;
    private static final Long NEWS_FEED_ITEM_ID = 100L;

    @Mock private NewsFeedItemRepository repository;
    @Mock private TenantContextHolder tenantContextHolder;

    private DeleteNewsFeedItemUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new DeleteNewsFeedItemUseCase(repository, tenantContextHolder);
    }

    @Nested
    @DisplayName("Successful Deletion")
    class SuccessfulDeletion {

        @Test
        @DisplayName("Should soft-delete news feed item when found by composite key")
        void shouldSoftDeleteNewsFeedItem_whenFoundByCompositeKey() {
            // Given
            when(tenantContextHolder.requireTenantId()).thenReturn(TENANT_ID);
            NewsFeedItemDataModel entity = new NewsFeedItemDataModel();
            NewsFeedItemDataModel.NewsFeedItemCompositeId compositeId =
                    new NewsFeedItemDataModel.NewsFeedItemCompositeId(TENANT_ID, NEWS_FEED_ITEM_ID);
            when(repository.findById(compositeId)).thenReturn(Optional.of(entity));

            // When
            useCase.delete(NEWS_FEED_ITEM_ID);

            // Then
            InOrder inOrder = inOrder(tenantContextHolder, repository);
            inOrder.verify(tenantContextHolder, times(1)).requireTenantId();
            inOrder.verify(repository, times(1)).findById(compositeId);
            inOrder.verify(repository, times(1)).delete(entity);
            inOrder.verifyNoMoreInteractions();
        }
    }

    @Nested
    @DisplayName("Entity Not Found")
    class EntityNotFound {

        @Test
        @DisplayName("Should throw EntityNotFoundException when news feed item missing")
        void shouldThrowEntityNotFound_whenNewsFeedItemMissing() {
            // Given
            when(tenantContextHolder.requireTenantId()).thenReturn(TENANT_ID);
            NewsFeedItemDataModel.NewsFeedItemCompositeId compositeId =
                    new NewsFeedItemDataModel.NewsFeedItemCompositeId(TENANT_ID, NEWS_FEED_ITEM_ID);
            when(repository.findById(compositeId)).thenReturn(Optional.empty());

            // When / Then
            assertThatThrownBy(() -> useCase.delete(NEWS_FEED_ITEM_ID))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessage(String.format(EntityNotFoundException.MESSAGE_TEMPLATE,
                            EntityType.NEWS_FEED_ITEM, String.valueOf(NEWS_FEED_ITEM_ID)));

            // Rule 9 — verify downstream delete was NOT called
            verify(tenantContextHolder, times(1)).requireTenantId();
            verify(repository, times(1)).findById(compositeId);
            verifyNoMoreInteractions(repository, tenantContextHolder);
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
            NewsFeedItemDataModel entity = new NewsFeedItemDataModel();
            NewsFeedItemDataModel.NewsFeedItemCompositeId compositeId =
                    new NewsFeedItemDataModel.NewsFeedItemCompositeId(TENANT_ID, NEWS_FEED_ITEM_ID);
            when(repository.findById(compositeId)).thenReturn(Optional.of(entity));
            doThrow(new DataIntegrityViolationException("FK constraint"))
                    .when(repository).delete(entity);

            // When / Then
            assertThatThrownBy(() -> useCase.delete(NEWS_FEED_ITEM_ID))
                    .isInstanceOf(EntityDeletionNotAllowedException.class)
                    .hasMessage(String.format(EntityDeletionNotAllowedException.MESSAGE_TEMPLATE,
                            EntityType.NEWS_FEED_ITEM, String.valueOf(NEWS_FEED_ITEM_ID)));

            // Rule 9 — verify all collaborators called in order
            verify(tenantContextHolder, times(1)).requireTenantId();
            verify(repository, times(1)).findById(compositeId);
            verify(repository, times(1)).delete(entity);
            verifyNoMoreInteractions(repository, tenantContextHolder);
        }
    }
}
