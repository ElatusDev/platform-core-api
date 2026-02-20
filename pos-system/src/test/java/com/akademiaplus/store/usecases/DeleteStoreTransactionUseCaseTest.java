/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.store.usecases;

import com.akademiaplus.billing.store.StoreTransactionDataModel;
import com.akademiaplus.infra.persistence.config.TenantContextHolder;
import com.akademiaplus.store.interfaceadapters.StoreTransactionRepository;
import com.akademiaplus.utilities.EntityType;
import com.akademiaplus.utilities.exceptions.EntityDeletionNotAllowedException;
import com.akademiaplus.utilities.exceptions.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link DeleteStoreTransactionUseCase}.
 *
 * @author ElatusDev
 * @since 1.0
 */
@DisplayName("DeleteStoreTransactionUseCase")
@ExtendWith(MockitoExtension.class)
class DeleteStoreTransactionUseCaseTest {

    private static final Long TENANT_ID = 1L;
    private static final Long STORE_TRANSACTION_ID = 200L;

    @Mock
    private StoreTransactionRepository repository;

    @Mock
    private TenantContextHolder tenantContextHolder;

    private DeleteStoreTransactionUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new DeleteStoreTransactionUseCase(repository, tenantContextHolder);
    }

    @Nested
    @DisplayName("Successful Deletion")
    class SuccessfulDeletion {

        @Test
        @DisplayName("Should soft-delete store transaction when found by composite key")
        void shouldSoftDeleteStoreTransaction_whenFoundByCompositeKey() {
            // Given
            when(tenantContextHolder.requireTenantId()).thenReturn(TENANT_ID);
            StoreTransactionDataModel entity = new StoreTransactionDataModel();
            StoreTransactionDataModel.StoreTransactionCompositeId compositeId =
                    new StoreTransactionDataModel.StoreTransactionCompositeId(TENANT_ID, STORE_TRANSACTION_ID);
            when(repository.findById(compositeId)).thenReturn(Optional.of(entity));

            // When
            useCase.delete(STORE_TRANSACTION_ID);

            // Then
            verify(repository).delete(entity);
        }
    }

    @Nested
    @DisplayName("Entity Not Found")
    class EntityNotFound {

        @Test
        @DisplayName("Should throw EntityNotFoundException when store transaction missing")
        void shouldThrowEntityNotFound_whenStoreTransactionMissing() {
            // Given
            when(tenantContextHolder.requireTenantId()).thenReturn(TENANT_ID);
            StoreTransactionDataModel.StoreTransactionCompositeId compositeId =
                    new StoreTransactionDataModel.StoreTransactionCompositeId(TENANT_ID, STORE_TRANSACTION_ID);
            when(repository.findById(compositeId)).thenReturn(Optional.empty());

            // When / Then
            assertThatThrownBy(() -> useCase.delete(STORE_TRANSACTION_ID))
                    .isInstanceOf(EntityNotFoundException.class)
                    .satisfies(ex -> {
                        EntityNotFoundException enfe = (EntityNotFoundException) ex;
                        assertThat(enfe.getEntityType()).isEqualTo(EntityType.STORE_TRANSACTION);
                        assertThat(enfe.getEntityId()).isEqualTo(String.valueOf(STORE_TRANSACTION_ID));
                    });
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
            StoreTransactionDataModel entity = new StoreTransactionDataModel();
            StoreTransactionDataModel.StoreTransactionCompositeId compositeId =
                    new StoreTransactionDataModel.StoreTransactionCompositeId(TENANT_ID, STORE_TRANSACTION_ID);
            when(repository.findById(compositeId)).thenReturn(Optional.of(entity));
            doThrow(new DataIntegrityViolationException("FK constraint"))
                    .when(repository).delete(entity);

            // When / Then
            assertThatThrownBy(() -> useCase.delete(STORE_TRANSACTION_ID))
                    .isInstanceOf(EntityDeletionNotAllowedException.class)
                    .satisfies(ex -> {
                        EntityDeletionNotAllowedException edna =
                                (EntityDeletionNotAllowedException) ex;
                        assertThat(edna.getEntityType()).isEqualTo(EntityType.STORE_TRANSACTION);
                        assertThat(edna.getEntityId()).isEqualTo(String.valueOf(STORE_TRANSACTION_ID));
                        assertThat(edna.getReason()).isNull();
                    });
        }
    }
}
