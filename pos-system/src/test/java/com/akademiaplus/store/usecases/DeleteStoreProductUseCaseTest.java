/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.store.usecases;

import com.akademiaplus.billing.store.StoreProductDataModel;
import com.akademiaplus.infra.persistence.config.TenantContextHolder;
import com.akademiaplus.store.interfaceadapters.StoreProductRepository;
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
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link DeleteStoreProductUseCase}.
 *
 * @author ElatusDev
 * @since 1.0
 */
@DisplayName("DeleteStoreProductUseCase")
@ExtendWith(MockitoExtension.class)
class DeleteStoreProductUseCaseTest {

    private static final Long TENANT_ID = 1L;
    private static final Long STORE_PRODUCT_ID = 100L;

    @Mock
    private StoreProductRepository repository;

    @Mock
    private TenantContextHolder tenantContextHolder;

    private DeleteStoreProductUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new DeleteStoreProductUseCase(repository, tenantContextHolder);
    }

    @Nested
    @DisplayName("Successful Deletion")
    class SuccessfulDeletion {

        @Test
        @DisplayName("Should soft-delete store product when found by composite key")
        void shouldSoftDeleteStoreProduct_whenFoundByCompositeKey() {
            // Given
            when(tenantContextHolder.requireTenantId()).thenReturn(TENANT_ID);
            StoreProductDataModel entity = new StoreProductDataModel();
            StoreProductDataModel.ProductCompositeId compositeId =
                    new StoreProductDataModel.ProductCompositeId(TENANT_ID, STORE_PRODUCT_ID);
            when(repository.findById(compositeId)).thenReturn(Optional.of(entity));

            // When
            useCase.delete(STORE_PRODUCT_ID);

            // Then
            assertThat(entity).isNotNull();
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
        @DisplayName("Should throw EntityNotFoundException when store product missing")
        void shouldThrowEntityNotFound_whenStoreProductMissing() {
            // Given
            when(tenantContextHolder.requireTenantId()).thenReturn(TENANT_ID);
            StoreProductDataModel.ProductCompositeId compositeId =
                    new StoreProductDataModel.ProductCompositeId(TENANT_ID, STORE_PRODUCT_ID);
            when(repository.findById(compositeId)).thenReturn(Optional.empty());

            // When / Then
            assertThatThrownBy(() -> useCase.delete(STORE_PRODUCT_ID))
                    .isInstanceOf(EntityNotFoundException.class)
                    .satisfies(ex -> {
                        EntityNotFoundException enfe = (EntityNotFoundException) ex;
                        assertThat(enfe.getEntityType()).isEqualTo(EntityType.STORE_PRODUCT);
                        assertThat(enfe.getEntityId()).isEqualTo(String.valueOf(STORE_PRODUCT_ID));
                    });

            InOrder inOrder = inOrder(tenantContextHolder, repository);
            inOrder.verify(tenantContextHolder, times(1)).requireTenantId();
            inOrder.verify(repository, times(1)).findById(compositeId);
            inOrder.verifyNoMoreInteractions();
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
            StoreProductDataModel entity = new StoreProductDataModel();
            StoreProductDataModel.ProductCompositeId compositeId =
                    new StoreProductDataModel.ProductCompositeId(TENANT_ID, STORE_PRODUCT_ID);
            when(repository.findById(compositeId)).thenReturn(Optional.of(entity));
            doThrow(new DataIntegrityViolationException("FK constraint"))
                    .when(repository).delete(entity);

            // When / Then
            assertThatThrownBy(() -> useCase.delete(STORE_PRODUCT_ID))
                    .isInstanceOf(EntityDeletionNotAllowedException.class)
                    .satisfies(ex -> {
                        EntityDeletionNotAllowedException edna =
                                (EntityDeletionNotAllowedException) ex;
                        assertThat(edna.getEntityType()).isEqualTo(EntityType.STORE_PRODUCT);
                        assertThat(edna.getEntityId()).isEqualTo(String.valueOf(STORE_PRODUCT_ID));
                        assertThat(edna.getReason()).isNull();
                    });

            InOrder inOrder = inOrder(tenantContextHolder, repository);
            inOrder.verify(tenantContextHolder, times(1)).requireTenantId();
            inOrder.verify(repository, times(1)).findById(compositeId);
            inOrder.verify(repository, times(1)).delete(entity);
            inOrder.verifyNoMoreInteractions();
        }
    }
}
