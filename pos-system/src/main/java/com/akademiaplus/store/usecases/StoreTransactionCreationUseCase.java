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
import lombok.RequiredArgsConstructor;
import openapi.akademiaplus.domain.pos.system.dto.SaleItemRequestDTO;
import openapi.akademiaplus.domain.pos.system.dto.StoreTransactionCreationRequestDTO;
import openapi.akademiaplus.domain.pos.system.dto.StoreTransactionCreationResponseDTO;
import org.modelmapper.ModelMapper;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Map;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Handles store transaction creation including sale item resolution,
 * price capture, stock validation, and inventory adjustment.
 * <p>
 * Uses a named TypeMap ({@value MAP_NAME}) for header-level DTO→entity
 * mapping. Sale items are built manually after product resolution.
 *
 * @author ElatusDev
 * @since 1.0
 */
@Service
@RequiredArgsConstructor
public class StoreTransactionCreationUseCase {

    /** Named TypeMap for header-level DTO→entity mapping. */
    public static final String MAP_NAME = "storeTransactionMap";

    /** Error message when sale items are null or empty. */
    public static final String ERROR_EMPTY_SALE_ITEMS =
            "Transaction must contain at least one sale item";

    /** Error message when a product has insufficient stock. */
    public static final String ERROR_INSUFFICIENT_STOCK =
            "Insufficient stock for product '%s' (ID: %d): requested %d, available %d";

    /** Transaction type constant for sales. */
    public static final String TRANSACTION_TYPE_SALE = "SALE";

    /** Transaction type constant for refunds. */
    public static final String TRANSACTION_TYPE_REFUND = "REFUND";

    /** JWT claim key for the employee ID. */
    public static final String CLAIM_EMPLOYEE_ID = "employeeId";

    private final ApplicationContext applicationContext;
    private final StoreTransactionRepository storeTransactionRepository;
    private final StoreProductRepository storeProductRepository;
    private final ModelMapper modelMapper;

    /**
     * Creates a store transaction with sale items, computing the total
     * server-side and adjusting product inventory.
     *
     * @param dto the creation request containing transaction header and sale items
     * @return response containing the persisted transaction ID
     * @throws IllegalArgumentException if sale items are empty or stock is insufficient
     * @throws EntityNotFoundException  if a referenced product does not exist
     */
    @Transactional
    public StoreTransactionCreationResponseDTO create(StoreTransactionCreationRequestDTO dto) {
        validateSaleItems(dto);

        StoreTransactionDataModel transaction = transform(dto);
        transaction.setEmployeeId(extractEmployeeId());
        List<StoreSaleItemDataModel> saleItems = buildSaleItems(dto, transaction);
        transaction.setSaleItems(saleItems);
        transaction.setTotalAmount(computeTotalAmount(saleItems));

        StoreTransactionDataModel saved = storeTransactionRepository.saveAndFlush(transaction);
        adjustInventory(saleItems, dto.getTransactionType(), transaction.getTenantId());

        return modelMapper.map(saved, StoreTransactionCreationResponseDTO.class);
    }

    /**
     * Transforms the DTO into a transaction entity via the named TypeMap.
     *
     * @param dto the creation request DTO
     * @return a prototype-scoped transaction entity with header fields mapped
     */
    public StoreTransactionDataModel transform(StoreTransactionCreationRequestDTO dto) {
        final StoreTransactionDataModel model =
                applicationContext.getBean(StoreTransactionDataModel.class);
        modelMapper.map(dto, model, MAP_NAME);
        return model;
    }

    /**
     * Validates that the request contains at least one sale item.
     *
     * @param dto the creation request to validate
     * @throws IllegalArgumentException if sale items are null or empty
     */
    private void validateSaleItems(StoreTransactionCreationRequestDTO dto) {
        if (dto.getSaleItems() == null || dto.getSaleItems().isEmpty()) {
            throw new IllegalArgumentException(ERROR_EMPTY_SALE_ITEMS);
        }
    }

    /**
     * Builds sale item entities by resolving each referenced product,
     * capturing price-at-sale, and validating stock for SALE transactions.
     *
     * @param dto         the creation request containing sale item DTOs
     * @param transaction the parent transaction entity
     * @return assembled list of sale item entities
     * @throws EntityNotFoundException  if a referenced product does not exist
     * @throws IllegalArgumentException if stock is insufficient for a SALE
     */
    private List<StoreSaleItemDataModel> buildSaleItems(
            StoreTransactionCreationRequestDTO dto,
            StoreTransactionDataModel transaction) {

        Long tenantId = transaction.getTenantId();
        List<StoreSaleItemDataModel> items = new ArrayList<>();

        for (SaleItemRequestDTO itemDto : dto.getSaleItems()) {
            StoreProductDataModel product = storeProductRepository
                    .findById(new StoreProductDataModel.ProductCompositeId(
                            tenantId, itemDto.getStoreProductId()))
                    .orElseThrow(() -> new EntityNotFoundException(
                            EntityType.STORE_PRODUCT,
                            String.valueOf(itemDto.getStoreProductId())));

            if (TRANSACTION_TYPE_SALE.equals(dto.getTransactionType())) {
                validateStock(product, itemDto.getQuantity());
            }

            StoreSaleItemDataModel saleItem =
                    applicationContext.getBean(StoreSaleItemDataModel.class);
            saleItem.setStoreProductId(itemDto.getStoreProductId());
            saleItem.setQuantity(itemDto.getQuantity());
            saleItem.setUnitPriceAtSale(product.getPrice());
            saleItem.setItemTotal(product.getPrice()
                    .multiply(BigDecimal.valueOf(itemDto.getQuantity())));
            saleItem.setTransaction(transaction);

            items.add(saleItem);
        }
        return items;
    }

    /**
     * Validates that a product has sufficient stock for the requested quantity.
     *
     * @param product           the product to check
     * @param requestedQuantity the quantity requested
     * @throws IllegalArgumentException if stock is insufficient
     */
    private void validateStock(StoreProductDataModel product, Integer requestedQuantity) {
        if (product.getStockQuantity() < requestedQuantity) {
            throw new IllegalArgumentException(String.format(
                    ERROR_INSUFFICIENT_STOCK,
                    product.getName(),
                    product.getStoreProductId(),
                    requestedQuantity,
                    product.getStockQuantity()));
        }
    }

    /**
     * Computes the transaction total from the sum of all sale item totals.
     *
     * @param saleItems the list of sale items
     * @return the total amount as BigDecimal
     */
    private BigDecimal computeTotalAmount(List<StoreSaleItemDataModel> saleItems) {
        return saleItems.stream()
                .map(StoreSaleItemDataModel::getItemTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Adjusts inventory for each sale item based on transaction type.
     * SALE decrements stock; REFUND increments stock.
     *
     * @param saleItems       the sale items to adjust inventory for
     * @param transactionType the transaction type (SALE or REFUND)
     * @param tenantId        the tenant context for product lookups
     */
    private void adjustInventory(List<StoreSaleItemDataModel> saleItems,
                                  String transactionType, Long tenantId) {
        for (StoreSaleItemDataModel item : saleItems) {
            StoreProductDataModel product = storeProductRepository.findById(
                    new StoreProductDataModel.ProductCompositeId(
                            tenantId, item.getStoreProductId()))
                    .orElseThrow();

            int adjustment = item.getQuantity();
            if (TRANSACTION_TYPE_SALE.equals(transactionType)) {
                product.setStockQuantity(product.getStockQuantity() - adjustment);
            } else if (TRANSACTION_TYPE_REFUND.equals(transactionType)) {
                product.setStockQuantity(product.getStockQuantity() + adjustment);
            }
            storeProductRepository.saveAndFlush(product);
        }
    }

    /**
     * Extracts the employee ID from the current security context, if available.
     * <p>
     * Returns {@code null} for unauthenticated requests or when the JWT
     * does not contain an {@value CLAIM_EMPLOYEE_ID} claim. This supports
     * self-service kiosk scenarios where no employee is involved.
     * <p>
     * The current JWT structure stores employee ID as an additional claim.
     * If the claim is absent, the method returns {@code null} gracefully.
     *
     * @return the employee ID from the JWT, or {@code null}
     */
    Long extractEmployeeId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return null;
        }
        if (auth instanceof UsernamePasswordAuthenticationToken token) {
            Object details = token.getDetails();
            if (details instanceof Map<?, ?> claims) {
                Object employeeIdClaim = claims.get(CLAIM_EMPLOYEE_ID);
                if (employeeIdClaim != null) {
                    return Long.valueOf(employeeIdClaim.toString());
                }
            }
        }
        return null;
    }
}
