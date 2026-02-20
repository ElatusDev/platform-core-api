/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.customer.adultstudent.usecases;

import com.akademiaplus.customer.adultstudent.interfaceadapters.AdultStudentRepository;
import com.akademiaplus.exception.AdultStudentDeletionNotAllowedException;
import com.akademiaplus.exception.AdultStudentNotFoundException;
import com.akademiaplus.infra.persistence.config.TenantContextHolder;
import com.akademiaplus.users.customer.AdultStudentDataModel;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class DeleteAdultStudentUseCase {
    private final AdultStudentRepository adultStudentRepository;
    private final TenantContextHolder tenantContextHolder;

    public DeleteAdultStudentUseCase(AdultStudentRepository adultStudentRepository,
                                      TenantContextHolder tenantContextHolder) {
        this.adultStudentRepository = adultStudentRepository;
        this.tenantContextHolder = tenantContextHolder;
    }

    public void delete(Long adultStudentId) {
        Long tenantId = tenantContextHolder.getTenantId()
                .orElseThrow(() -> new IllegalArgumentException("Tenant context is required"));
        Optional<AdultStudentDataModel> result = adultStudentRepository.findById(
                new AdultStudentDataModel.AdultStudentCompositeId(tenantId, adultStudentId));
        if(result.isPresent()) {
            try {
                adultStudentRepository.delete(result.get());
            } catch(DataIntegrityViolationException ex) {
                throw new AdultStudentDeletionNotAllowedException(ex);
            }
        } else {
            throw new AdultStudentNotFoundException(String.valueOf(adultStudentId));
        }
    }
}
