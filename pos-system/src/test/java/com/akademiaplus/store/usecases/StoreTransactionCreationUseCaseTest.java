/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.store.usecases;

import com.akademiaplus.billing.store.StoreSaleItemDataModel;
import com.akademiaplus.billing.store.StoreProductDataModel;
import com.akademiaplus.billing.store.StoreTransactionDataModel;
import com.akademiaplus.store.interfaceadapters.StoreProductRepository;
import com.akademiaplus.store.interfaceadapters.StoreTransactionRepository;
import com.akademiaplus.utilities.EntityType;
import com.akademiaplus.utilities.exceptions.EntityNotFoundException;
import openapi.akademiaplus.domain.pos.system.dto.SaleItemRequestDTO;
import openapi.akademiaplus.domain.pos.system.dto.StoreTransactionCreationRequestDTO;
import openapi.akademiaplus.domain.pos.system.dto.StoreTransactionCreationResponseDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.springframework.context.ApplicationContext;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link StoreTransactionCreationUseCase}.
 */
@DisplayName("StoreTransactionCreationUseCase")
@ExtendWith(MockitoExtension.class)
class StoreTransactionCreationUseCaseTest {

    private static final Long TENANT_ID = 1L;
    private static final Long PRODUCT_ID_1 = 10L;
    private static final Long PRODUCT_ID_2 = 20L;
    private static final String PRODUCT_NAME_1 = "Notebook";
    private static final String PRODUCT_NAME_2 = "Pen";
    private static final BigDecimal PRODUCT_PRICE_1 = new BigDecimal("5.00");
    private static final BigDecimal PRODUCT_PRICE_2 = new BigDecimal("2.50");
    private static final Integer STOCK_1 = 100;
    private static final Integer STOCK_2 = 50;
    private static final Integer QUANTITY_1 = 3;
    private static final Integer QUANTITY_2 = 5;
    private static final String PAYMENT_METHOD = "CASH";
    private static final Long SAVED_TRANSACTION_ID = 99L;

    @Mock private ApplicationContext applicationContext;
    @Mock private StoreTransactionRepository storeTransactionRepository;
    @Mock private StoreProductRepository storeProductRepository;
    @Mock private ModelMapper modelMapper;

    private StoreTransactionCreationUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new StoreTransactionCreationUseCase(
                applicationContext, storeTransactionRepository,
                storeProductRepository, modelMapper);
    }

    private StoreTransactionCreationRequestDTO buildDto(String transactionType,
                                                         SaleItemRequestDTO... items) {
        StoreTransactionCreationRequestDTO dto = new StoreTransactionCreationRequestDTO();
        dto.setTransactionType(transactionType);
        dto.setPaymentMethod(PAYMENT_METHOD);
        dto.setSaleItems(items == null ? null : List.of(items));
        return dto;
    }

    private SaleItemRequestDTO buildSaleItemDto(Long productId, Integer quantity) {
        SaleItemRequestDTO item = new SaleItemRequestDTO();
        item.setStoreProductId(productId);
        item.setQuantity(quantity);
        return item;
    }

    private StoreProductDataModel buildProduct(Long productId, String name,
                                                BigDecimal price, Integer stock) {
        StoreProductDataModel product = new StoreProductDataModel();
        product.setStoreProductId(productId);
        product.setName(name);
        product.setPrice(price);
        product.setStockQuantity(stock);
        return product;
    }

    private StoreProductDataModel.ProductCompositeId compositeId(Long productId) {
        return new StoreProductDataModel.ProductCompositeId(TENANT_ID, productId);
    }

    /**
     * Stubs the common transform flow: getBean returns the prototype,
     * modelMapper.map(dto, model, MAP_NAME) is a void no-op.
     */
    private StoreTransactionDataModel stubTransformFlow(StoreTransactionCreationRequestDTO dto) {
        StoreTransactionDataModel transaction = new StoreTransactionDataModel();
        transaction.setTenantId(TENANT_ID);
        when(applicationContext.getBean(StoreTransactionDataModel.class)).thenReturn(transaction);
        doNothing().when(modelMapper).map(dto, transaction,
                StoreTransactionCreationUseCase.MAP_NAME);
        return transaction;
    }

    @Nested
    @DisplayName("Validation")
    class Validation {

        @Test
        @DisplayName("Should throw exception when sale items are null")
        void shouldThrowException_whenSaleItemsNull() {
            // Given
            StoreTransactionCreationRequestDTO dto = new StoreTransactionCreationRequestDTO();
            dto.setTransactionType(StoreTransactionCreationUseCase.TRANSACTION_TYPE_SALE);
            dto.setPaymentMethod(PAYMENT_METHOD);
            dto.setSaleItems(null);

            // When & Then
            assertThatThrownBy(() -> useCase.create(dto))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage(StoreTransactionCreationUseCase.ERROR_EMPTY_SALE_ITEMS);
        }

        @Test
        @DisplayName("Should throw exception when sale items are empty")
        void shouldThrowException_whenSaleItemsEmpty() {
            // Given
            StoreTransactionCreationRequestDTO dto = new StoreTransactionCreationRequestDTO();
            dto.setTransactionType(StoreTransactionCreationUseCase.TRANSACTION_TYPE_SALE);
            dto.setPaymentMethod(PAYMENT_METHOD);
            dto.setSaleItems(Collections.emptyList());

            // When & Then
            assertThatThrownBy(() -> useCase.create(dto))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage(StoreTransactionCreationUseCase.ERROR_EMPTY_SALE_ITEMS);
        }
    }

    @Nested
    @DisplayName("Product Resolution")
    class ProductResolution {

        @Test
        @DisplayName("Should throw EntityNotFoundException when product ID is invalid")
        void shouldThrowEntityNotFound_whenProductIdInvalid() {
            // Given
            SaleItemRequestDTO item = buildSaleItemDto(PRODUCT_ID_1, QUANTITY_1);
            StoreTransactionCreationRequestDTO dto = buildDto(
                    StoreTransactionCreationUseCase.TRANSACTION_TYPE_SALE, item);
            StoreTransactionDataModel transaction = stubTransformFlow(dto);

            when(storeProductRepository.findById(compositeId(PRODUCT_ID_1)))
                    .thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> useCase.create(dto))
                    .isInstanceOf(EntityNotFoundException.class)
                    .satisfies(ex -> {
                        EntityNotFoundException enfe = (EntityNotFoundException) ex;
                        assertThat(enfe.getEntityType()).isEqualTo(EntityType.STORE_PRODUCT);
                        assertThat(enfe.getEntityId())
                                .isEqualTo(String.valueOf(PRODUCT_ID_1));
                    });
        }
    }

    @Nested
    @DisplayName("Stock Validation")
    class StockValidation {

        @Test
        @DisplayName("Should throw exception when insufficient stock for sale")
        void shouldThrowException_whenInsufficientStockForSale() {
            // Given
            Integer insufficientStock = 2;
            SaleItemRequestDTO item = buildSaleItemDto(PRODUCT_ID_1, QUANTITY_1);
            StoreTransactionCreationRequestDTO dto = buildDto(
                    StoreTransactionCreationUseCase.TRANSACTION_TYPE_SALE, item);
            StoreTransactionDataModel transaction = stubTransformFlow(dto);

            StoreProductDataModel product = buildProduct(
                    PRODUCT_ID_1, PRODUCT_NAME_1, PRODUCT_PRICE_1, insufficientStock);
            when(storeProductRepository.findById(compositeId(PRODUCT_ID_1)))
                    .thenReturn(Optional.of(product));

            // When & Then
            assertThatThrownBy(() -> useCase.create(dto))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage(String.format(
                            StoreTransactionCreationUseCase.ERROR_INSUFFICIENT_STOCK,
                            PRODUCT_NAME_1, PRODUCT_ID_1, QUANTITY_1, insufficientStock));
        }

        @Test
        @DisplayName("Should skip stock validation when transaction type is refund")
        void shouldSkipStockValidation_whenTransactionTypeRefund() {
            // Given
            Integer lowStock = 1;
            SaleItemRequestDTO item = buildSaleItemDto(PRODUCT_ID_1, QUANTITY_1);
            StoreTransactionCreationRequestDTO dto = buildDto(
                    StoreTransactionCreationUseCase.TRANSACTION_TYPE_REFUND, item);
            StoreTransactionDataModel transaction = stubTransformFlow(dto);

            StoreProductDataModel product = buildProduct(
                    PRODUCT_ID_1, PRODUCT_NAME_1, PRODUCT_PRICE_1, lowStock);
            when(storeProductRepository.findById(compositeId(PRODUCT_ID_1)))
                    .thenReturn(Optional.of(product));

            StoreSaleItemDataModel saleItem = new StoreSaleItemDataModel();
            when(applicationContext.getBean(StoreSaleItemDataModel.class)).thenReturn(saleItem);

            StoreTransactionDataModel savedTransaction = new StoreTransactionDataModel();
            savedTransaction.setStoreTransactionId(SAVED_TRANSACTION_ID);
            when(storeTransactionRepository.saveAndFlush(transaction)).thenReturn(savedTransaction);

            StoreTransactionCreationResponseDTO responseDto =
                    new StoreTransactionCreationResponseDTO();
            responseDto.setStoreTransactionId(SAVED_TRANSACTION_ID);
            when(modelMapper.map(savedTransaction, StoreTransactionCreationResponseDTO.class))
                    .thenReturn(responseDto);

            // adjustInventory re-fetches the product
            when(storeProductRepository.findById(compositeId(PRODUCT_ID_1)))
                    .thenReturn(Optional.of(product));
            when(storeProductRepository.saveAndFlush(product)).thenReturn(product);

            // When
            StoreTransactionCreationResponseDTO result = useCase.create(dto);

            // Then — no exception thrown, stock validation was skipped
            assertThat(result.getStoreTransactionId()).isEqualTo(SAVED_TRANSACTION_ID);
        }
    }

    @Nested
    @DisplayName("Price Capture and Total Computation")
    class PriceCaptureAndTotal {

        @Test
        @DisplayName("Should capture current product price as unit price at sale")
        void shouldCaptureCurrentProductPrice_asUnitPriceAtSale() {
            // Given
            SaleItemRequestDTO item = buildSaleItemDto(PRODUCT_ID_1, QUANTITY_1);
            StoreTransactionCreationRequestDTO dto = buildDto(
                    StoreTransactionCreationUseCase.TRANSACTION_TYPE_SALE, item);
            StoreTransactionDataModel transaction = stubTransformFlow(dto);

            StoreProductDataModel product = buildProduct(
                    PRODUCT_ID_1, PRODUCT_NAME_1, PRODUCT_PRICE_1, STOCK_1);
            when(storeProductRepository.findById(compositeId(PRODUCT_ID_1)))
                    .thenReturn(Optional.of(product));

            StoreSaleItemDataModel saleItem = new StoreSaleItemDataModel();
            when(applicationContext.getBean(StoreSaleItemDataModel.class)).thenReturn(saleItem);

            StoreTransactionDataModel savedTransaction = new StoreTransactionDataModel();
            when(storeTransactionRepository.saveAndFlush(transaction)).thenReturn(savedTransaction);
            when(modelMapper.map(savedTransaction, StoreTransactionCreationResponseDTO.class))
                    .thenReturn(new StoreTransactionCreationResponseDTO());

            // adjustInventory re-fetches
            when(storeProductRepository.findById(compositeId(PRODUCT_ID_1)))
                    .thenReturn(Optional.of(product));
            when(storeProductRepository.saveAndFlush(product)).thenReturn(product);

            // When
            useCase.create(dto);

            // Then
            assertThat(saleItem.getUnitPriceAtSale()).isEqualByComparingTo(PRODUCT_PRICE_1);
        }

        @Test
        @DisplayName("Should compute item total as quantity times unit price")
        void shouldComputeItemTotal_asQuantityTimesUnitPrice() {
            // Given
            SaleItemRequestDTO item = buildSaleItemDto(PRODUCT_ID_1, QUANTITY_1);
            StoreTransactionCreationRequestDTO dto = buildDto(
                    StoreTransactionCreationUseCase.TRANSACTION_TYPE_SALE, item);
            StoreTransactionDataModel transaction = stubTransformFlow(dto);

            StoreProductDataModel product = buildProduct(
                    PRODUCT_ID_1, PRODUCT_NAME_1, PRODUCT_PRICE_1, STOCK_1);
            when(storeProductRepository.findById(compositeId(PRODUCT_ID_1)))
                    .thenReturn(Optional.of(product));

            StoreSaleItemDataModel saleItem = new StoreSaleItemDataModel();
            when(applicationContext.getBean(StoreSaleItemDataModel.class)).thenReturn(saleItem);

            StoreTransactionDataModel savedTransaction = new StoreTransactionDataModel();
            when(storeTransactionRepository.saveAndFlush(transaction)).thenReturn(savedTransaction);
            when(modelMapper.map(savedTransaction, StoreTransactionCreationResponseDTO.class))
                    .thenReturn(new StoreTransactionCreationResponseDTO());

            when(storeProductRepository.findById(compositeId(PRODUCT_ID_1)))
                    .thenReturn(Optional.of(product));
            when(storeProductRepository.saveAndFlush(product)).thenReturn(product);

            // When
            useCase.create(dto);

            // Then — 5.00 × 3 = 15.00
            BigDecimal expectedItemTotal = PRODUCT_PRICE_1
                    .multiply(BigDecimal.valueOf(QUANTITY_1));
            assertThat(saleItem.getItemTotal()).isEqualByComparingTo(expectedItemTotal);
        }

        @Test
        @DisplayName("Should compute transaction total as sum of item totals")
        void shouldComputeTransactionTotal_asSumOfItemTotals() {
            // Given
            SaleItemRequestDTO item1 = buildSaleItemDto(PRODUCT_ID_1, QUANTITY_1);
            SaleItemRequestDTO item2 = buildSaleItemDto(PRODUCT_ID_2, QUANTITY_2);
            StoreTransactionCreationRequestDTO dto = buildDto(
                    StoreTransactionCreationUseCase.TRANSACTION_TYPE_SALE, item1, item2);
            StoreTransactionDataModel transaction = stubTransformFlow(dto);

            StoreProductDataModel product1 = buildProduct(
                    PRODUCT_ID_1, PRODUCT_NAME_1, PRODUCT_PRICE_1, STOCK_1);
            StoreProductDataModel product2 = buildProduct(
                    PRODUCT_ID_2, PRODUCT_NAME_2, PRODUCT_PRICE_2, STOCK_2);

            when(storeProductRepository.findById(compositeId(PRODUCT_ID_1)))
                    .thenReturn(Optional.of(product1));
            when(storeProductRepository.findById(compositeId(PRODUCT_ID_2)))
                    .thenReturn(Optional.of(product2));

            StoreSaleItemDataModel saleItem1 = new StoreSaleItemDataModel();
            StoreSaleItemDataModel saleItem2 = new StoreSaleItemDataModel();
            when(applicationContext.getBean(StoreSaleItemDataModel.class))
                    .thenReturn(saleItem1)
                    .thenReturn(saleItem2);

            StoreTransactionDataModel savedTransaction = new StoreTransactionDataModel();
            when(storeTransactionRepository.saveAndFlush(transaction)).thenReturn(savedTransaction);
            when(modelMapper.map(savedTransaction, StoreTransactionCreationResponseDTO.class))
                    .thenReturn(new StoreTransactionCreationResponseDTO());

            // adjustInventory re-fetches both products
            when(storeProductRepository.findById(compositeId(PRODUCT_ID_1)))
                    .thenReturn(Optional.of(product1));
            when(storeProductRepository.findById(compositeId(PRODUCT_ID_2)))
                    .thenReturn(Optional.of(product2));
            when(storeProductRepository.saveAndFlush(product1)).thenReturn(product1);
            when(storeProductRepository.saveAndFlush(product2)).thenReturn(product2);

            // When
            useCase.create(dto);

            // Then — (5.00 × 3) + (2.50 × 5) = 15.00 + 12.50 = 27.50
            BigDecimal expectedTotal = PRODUCT_PRICE_1.multiply(BigDecimal.valueOf(QUANTITY_1))
                    .add(PRODUCT_PRICE_2.multiply(BigDecimal.valueOf(QUANTITY_2)));
            assertThat(transaction.getTotalAmount()).isEqualByComparingTo(expectedTotal);
        }
    }

    @Nested
    @DisplayName("Inventory Adjustment")
    class InventoryAdjustment {

        @Test
        @DisplayName("Should decrement stock when transaction type is sale")
        void shouldDecrementStock_whenTransactionTypeSale() {
            // Given
            SaleItemRequestDTO item = buildSaleItemDto(PRODUCT_ID_1, QUANTITY_1);
            StoreTransactionCreationRequestDTO dto = buildDto(
                    StoreTransactionCreationUseCase.TRANSACTION_TYPE_SALE, item);
            StoreTransactionDataModel transaction = stubTransformFlow(dto);

            StoreProductDataModel product = buildProduct(
                    PRODUCT_ID_1, PRODUCT_NAME_1, PRODUCT_PRICE_1, STOCK_1);
            when(storeProductRepository.findById(compositeId(PRODUCT_ID_1)))
                    .thenReturn(Optional.of(product));

            StoreSaleItemDataModel saleItem = new StoreSaleItemDataModel();
            when(applicationContext.getBean(StoreSaleItemDataModel.class)).thenReturn(saleItem);

            StoreTransactionDataModel savedTransaction = new StoreTransactionDataModel();
            when(storeTransactionRepository.saveAndFlush(transaction)).thenReturn(savedTransaction);
            when(modelMapper.map(savedTransaction, StoreTransactionCreationResponseDTO.class))
                    .thenReturn(new StoreTransactionCreationResponseDTO());

            // adjustInventory re-fetches
            when(storeProductRepository.findById(compositeId(PRODUCT_ID_1)))
                    .thenReturn(Optional.of(product));
            when(storeProductRepository.saveAndFlush(product)).thenReturn(product);

            // When
            useCase.create(dto);

            // Then — stock should be 100 - 3 = 97
            assertThat(product.getStockQuantity()).isEqualTo(STOCK_1 - QUANTITY_1);
        }

        @Test
        @DisplayName("Should increment stock when transaction type is refund")
        void shouldIncrementStock_whenTransactionTypeRefund() {
            // Given
            SaleItemRequestDTO item = buildSaleItemDto(PRODUCT_ID_1, QUANTITY_1);
            StoreTransactionCreationRequestDTO dto = buildDto(
                    StoreTransactionCreationUseCase.TRANSACTION_TYPE_REFUND, item);
            StoreTransactionDataModel transaction = stubTransformFlow(dto);

            StoreProductDataModel product = buildProduct(
                    PRODUCT_ID_1, PRODUCT_NAME_1, PRODUCT_PRICE_1, STOCK_1);
            when(storeProductRepository.findById(compositeId(PRODUCT_ID_1)))
                    .thenReturn(Optional.of(product));

            StoreSaleItemDataModel saleItem = new StoreSaleItemDataModel();
            when(applicationContext.getBean(StoreSaleItemDataModel.class)).thenReturn(saleItem);

            StoreTransactionDataModel savedTransaction = new StoreTransactionDataModel();
            when(storeTransactionRepository.saveAndFlush(transaction)).thenReturn(savedTransaction);
            when(modelMapper.map(savedTransaction, StoreTransactionCreationResponseDTO.class))
                    .thenReturn(new StoreTransactionCreationResponseDTO());

            // adjustInventory re-fetches
            when(storeProductRepository.findById(compositeId(PRODUCT_ID_1)))
                    .thenReturn(Optional.of(product));
            when(storeProductRepository.saveAndFlush(product)).thenReturn(product);

            // When
            useCase.create(dto);

            // Then — stock should be 100 + 3 = 103
            assertThat(product.getStockQuantity()).isEqualTo(STOCK_1 + QUANTITY_1);
        }
    }

    @Nested
    @DisplayName("Persistence")
    class Persistence {

        @Test
        @DisplayName("Should save transaction with sale items")
        void shouldSaveTransactionWithSaleItems() {
            // Given
            SaleItemRequestDTO item = buildSaleItemDto(PRODUCT_ID_1, QUANTITY_1);
            StoreTransactionCreationRequestDTO dto = buildDto(
                    StoreTransactionCreationUseCase.TRANSACTION_TYPE_SALE, item);
            StoreTransactionDataModel transaction = stubTransformFlow(dto);

            StoreProductDataModel product = buildProduct(
                    PRODUCT_ID_1, PRODUCT_NAME_1, PRODUCT_PRICE_1, STOCK_1);
            when(storeProductRepository.findById(compositeId(PRODUCT_ID_1)))
                    .thenReturn(Optional.of(product));

            StoreSaleItemDataModel saleItem = new StoreSaleItemDataModel();
            when(applicationContext.getBean(StoreSaleItemDataModel.class)).thenReturn(saleItem);

            StoreTransactionDataModel savedTransaction = new StoreTransactionDataModel();
            when(storeTransactionRepository.saveAndFlush(transaction)).thenReturn(savedTransaction);
            when(modelMapper.map(savedTransaction, StoreTransactionCreationResponseDTO.class))
                    .thenReturn(new StoreTransactionCreationResponseDTO());

            when(storeProductRepository.findById(compositeId(PRODUCT_ID_1)))
                    .thenReturn(Optional.of(product));
            when(storeProductRepository.saveAndFlush(product)).thenReturn(product);

            // When
            useCase.create(dto);

            // Then
            verify(storeTransactionRepository).saveAndFlush(transaction);
            assertThat(transaction.getSaleItems()).hasSize(1);
            assertThat(transaction.getSaleItems().get(0)).isSameAs(saleItem);
        }

        @Test
        @DisplayName("Should return mapped response DTO")
        void shouldReturnMappedResponseDto() {
            // Given
            SaleItemRequestDTO item = buildSaleItemDto(PRODUCT_ID_1, QUANTITY_1);
            StoreTransactionCreationRequestDTO dto = buildDto(
                    StoreTransactionCreationUseCase.TRANSACTION_TYPE_SALE, item);
            StoreTransactionDataModel transaction = stubTransformFlow(dto);

            StoreProductDataModel product = buildProduct(
                    PRODUCT_ID_1, PRODUCT_NAME_1, PRODUCT_PRICE_1, STOCK_1);
            when(storeProductRepository.findById(compositeId(PRODUCT_ID_1)))
                    .thenReturn(Optional.of(product));

            StoreSaleItemDataModel saleItem = new StoreSaleItemDataModel();
            when(applicationContext.getBean(StoreSaleItemDataModel.class)).thenReturn(saleItem);

            StoreTransactionDataModel savedTransaction = new StoreTransactionDataModel();
            savedTransaction.setStoreTransactionId(SAVED_TRANSACTION_ID);
            when(storeTransactionRepository.saveAndFlush(transaction)).thenReturn(savedTransaction);

            StoreTransactionCreationResponseDTO expectedResponse =
                    new StoreTransactionCreationResponseDTO();
            expectedResponse.setStoreTransactionId(SAVED_TRANSACTION_ID);
            when(modelMapper.map(savedTransaction, StoreTransactionCreationResponseDTO.class))
                    .thenReturn(expectedResponse);

            when(storeProductRepository.findById(compositeId(PRODUCT_ID_1)))
                    .thenReturn(Optional.of(product));
            when(storeProductRepository.saveAndFlush(product)).thenReturn(product);

            // When
            StoreTransactionCreationResponseDTO result = useCase.create(dto);

            // Then
            assertThat(result.getStoreTransactionId()).isEqualTo(SAVED_TRANSACTION_ID);
        }
    }
}
