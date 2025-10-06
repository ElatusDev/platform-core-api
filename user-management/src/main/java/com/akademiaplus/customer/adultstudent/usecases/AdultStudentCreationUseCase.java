/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.customer.adultstudent.usecases;

import com.akademiaplus.users.base.PersonPIIDataModel;
import com.akademiaplus.users.customer.AdultStudentDataModel;
import com.akademiaplus.customer.adultstudent.interfaceadapters.AdultStudentRepository;
import com.akademiaplus.utilities.security.HashingService;
import com.akademiaplus.utilities.security.PiiNormalizer;
import openapi.akademiaplus.domain.user.management.dto.AdultStudentCreationRequestDTO;
import openapi.akademiaplus.domain.user.management.dto.AdultStudentCreationResponseDTO;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdultStudentCreationUseCase {
    private final AdultStudentRepository adultStudentRepository;
    private final ModelMapper modelMapper;
    private final HashingService hashingService;
    private final PiiNormalizer piiNormalizer;

    public AdultStudentCreationUseCase(AdultStudentRepository adultStudentRepository,
                                       ModelMapper modelMapper,
                                       HashingService hashingService,
                                       PiiNormalizer piiNormalizer) {
        this.adultStudentRepository = adultStudentRepository;
        this.modelMapper = modelMapper;
        this.hashingService = hashingService;
        this.piiNormalizer = piiNormalizer;
    }

    @Transactional
    public AdultStudentCreationResponseDTO create(AdultStudentCreationRequestDTO dto) {
        return modelMapper.map(adultStudentRepository.save(transform((dto))), AdultStudentCreationResponseDTO.class);
    }

    public AdultStudentDataModel transform(AdultStudentCreationRequestDTO dto) {
        AdultStudentDataModel model = modelMapper.map(dto, AdultStudentDataModel.class);

        final PersonPIIDataModel personPIIDataModel = modelMapper.map(dto, PersonPIIDataModel.class);
        model.setPersonPII(personPIIDataModel);

        String normalizedEmail = piiNormalizer.normalizeEmail(model.getPersonPII().getEmail());
        model.getPersonPII().setEmailHash(hashingService.generateHash(normalizedEmail));

        String normalizedPhone = piiNormalizer.normalizePhoneNumber(model.getPersonPII().getPhone());
        model.getPersonPII().setPhoneHash(hashingService.generateHash(normalizedPhone));
        return model;
    }
}
