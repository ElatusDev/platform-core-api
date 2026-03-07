/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.leadmanagement.usecases;

import com.akademiaplus.leadmanagement.DemoRequestDataModel;
import com.akademiaplus.leadmanagement.interfaceadapters.DemoRequestRepository;
import com.akademiaplus.utilities.EntityType;
import com.akademiaplus.utilities.exceptions.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Soft-deletes a demo request by its ID.
 *
 * @author ElatusDev
 * @since 1.0
 */
@Service
@RequiredArgsConstructor
public class DeleteDemoRequestUseCase {

    private final DemoRequestRepository demoRequestRepository;

    /**
     * Deletes the demo request with the given ID.
     *
     * @param id the demo request ID
     * @throws EntityNotFoundException if the demo request does not exist
     */
    @Transactional
    public void delete(Long id) {
        DemoRequestDataModel model = demoRequestRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(
                        EntityType.DEMO_REQUEST, id.toString()));
        demoRequestRepository.delete(model);
    }
}
