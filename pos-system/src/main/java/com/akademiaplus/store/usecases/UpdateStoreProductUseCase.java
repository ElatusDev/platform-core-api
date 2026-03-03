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
import com.akademiaplus.utilities.exceptions.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import openapi.akademiaplus.domain.pos.system.dto.StoreProductCreationRequestDTO;
import openapi.akademiaplus.domain.pos.system.dto.StoreProductCreationResponseDTO;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Handles updating an existing store product by mapping new field values
 * onto the persisted entity.
 * <p>
 * Uses a named TypeMap ({@value MAP_NAME}) that skips the entity ID field
 * to prevent overwriting the composite key during mapping.
 *
 * @author ElatusDev
 * @since 1.0
 */
@Service
@RequiredArgsConstructor
public class UpdateStoreProductUseCase {

    /**
     * Named TypeMap identifier for update mapping.
     * Registered in {@link com.akademiaplus.config.PosModelMapperConfiguration}.
     */
    public static final String MAP_NAME = "storeProductUpdateMap";

    private final StoreProductRepository storeProductRepository;
    private final TenantContextHolder tenantContextHolder;
    private final ModelMapper modelMapper;

    /**
     * Updates the store product identified by {@code storeProductId}
     * within the current tenant context.
     *
     * @param storeProductId the entity-specific product ID
     * @param dto            the updated field values
     * @return response containing the product ID
     * @throws EntityNotFoundException if no product exists with the given composite key
     * @throws com.akademiaplus.utilities.exceptions.InvalidTenantException
     *         if no tenant context is set
     */
    @Transactional
    public StoreProductCreationResponseDTO update(Long storeProductId,
                                                   StoreProductCreationRequestDTO dto) {
        Long tenantId = tenantContextHolder.requireTenantId();

        StoreProductDataModel existing = storeProductRepository
                .findById(new StoreProductDataModel.ProductCompositeId(tenantId, storeProductId))
                .orElseThrow(() -> new EntityNotFoundException(
                        EntityType.STORE_PRODUCT, String.valueOf(storeProductId)));

        modelMapper.map(dto, existing, MAP_NAME);
        StoreProductDataModel saved = storeProductRepository.saveAndFlush(existing);
        return modelMapper.map(saved, StoreProductCreationResponseDTO.class);
    }
}
