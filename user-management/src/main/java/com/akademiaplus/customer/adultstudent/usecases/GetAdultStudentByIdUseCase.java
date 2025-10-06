/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.customer.adultstudent.usecases;

import com.akademiaplus.customer.adultstudent.interfaceadapters.AdultStudentRepository;
import com.akademiaplus.exception.AdultStudentNotFoundException;
import com.akademiaplus.users.customer.AdultStudentDataModel;
import openapi.akademiaplus.domain.user.management.dto.GetAdultStudentResponseDTO;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class GetAdultStudentByIdUseCase {
    private final AdultStudentRepository adultStudentRepository;
    private final ModelMapper modelMapper;

    public GetAdultStudentByIdUseCase(AdultStudentRepository adultStudentRepository,
                                      ModelMapper modelMapper) {
        this.adultStudentRepository = adultStudentRepository;
        this.modelMapper = modelMapper;
    }

    public GetAdultStudentResponseDTO get(Integer adultStudentId) {
        Optional<AdultStudentDataModel> result = adultStudentRepository.findById(adultStudentId);
        if(result.isPresent()) {
            AdultStudentDataModel found = result.get();
            GetAdultStudentResponseDTO dto = modelMapper.map(found, GetAdultStudentResponseDTO.class);
            modelMapper.map(found.getPersonPII(), dto);
            return dto;
        } else {
            throw new AdultStudentNotFoundException(String.valueOf(adultStudentId));
        }
    }
}
