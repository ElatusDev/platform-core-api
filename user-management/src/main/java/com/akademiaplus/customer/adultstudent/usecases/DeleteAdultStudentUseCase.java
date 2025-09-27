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
import com.akademiaplus.users.customer.AdultStudentDataModel;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class DeleteAdultStudentUseCase {
    private final AdultStudentRepository adultStudentRepository;

    public DeleteAdultStudentUseCase(AdultStudentRepository adultStudentRepository) {
        this.adultStudentRepository = adultStudentRepository;
    }

    public void delete(Integer adultStudentId) {
        Optional<AdultStudentDataModel> result = adultStudentRepository.findById(adultStudentId);
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
