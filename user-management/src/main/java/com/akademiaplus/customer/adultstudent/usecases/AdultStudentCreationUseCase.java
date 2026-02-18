/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.customer.adultstudent.usecases;

import com.akademiaplus.security.CustomerAuthDataModel;
import com.akademiaplus.users.base.PersonPIIDataModel;
import com.akademiaplus.users.customer.AdultStudentDataModel;
import com.akademiaplus.customer.adultstudent.interfaceadapters.AdultStudentRepository;
import com.akademiaplus.utilities.security.HashingService;
import com.akademiaplus.utilities.security.PiiNormalizer;
import lombok.RequiredArgsConstructor;
import openapi.akademiaplus.domain.user.management.dto.AdultStudentCreationRequestDTO;
import openapi.akademiaplus.domain.user.management.dto.AdultStudentCreationResponseDTO;
import org.modelmapper.ModelMapper;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

/**
 * Use case responsible for creating adult students.
 * <p>
 * Uses a named TypeMap ({@value MAP_NAME}) to prevent ModelMapper from
 * deep-matching DTO fields into nested {@code personPII} and {@code customerAuth}
 * objects. These are mapped separately and wired manually.
 */
@RequiredArgsConstructor
@Service
public class AdultStudentCreationUseCase {
    public static final String MAP_NAME = "adultStudentMap";

    private final ApplicationContext applicationContext;
    private final AdultStudentRepository adultStudentRepository;
    private final ModelMapper modelMapper;
    private final HashingService hashingService;
    private final PiiNormalizer piiNormalizer;

    @Transactional
    public AdultStudentCreationResponseDTO create(AdultStudentCreationRequestDTO dto) {
        return modelMapper.map(adultStudentRepository.save(transform(dto)), AdultStudentCreationResponseDTO.class);
    }

    /**
     * Transforms an AdultStudent creation DTO into a persistence-ready data model.
     * <p>
     * PersonPII is mapped in a separate call. CustomerAuth is built manually
     * because DTO fields are plain Strings that need explicit wiring.
     * Entry date is set to today since the DTO does not carry it.
     *
     * @param dto the adult student creation request
     * @return populated AdultStudentDataModel ready for persistence
     */
    public AdultStudentDataModel transform(AdultStudentCreationRequestDTO dto) {
        final PersonPIIDataModel personPIIDataModel = applicationContext.getBean(PersonPIIDataModel.class);
        modelMapper.map(dto, personPIIDataModel);

        final AdultStudentDataModel model = applicationContext.getBean(AdultStudentDataModel.class);
        modelMapper.map(dto, model, MAP_NAME);
        model.setPersonPII(personPIIDataModel);
        model.setEntryDate(LocalDate.now());

        String normalizedEmail = piiNormalizer.normalizeEmail(model.getPersonPII().getEmail());
        model.getPersonPII().setEmailHash(hashingService.generateHash(normalizedEmail));

        String normalizedPhone = piiNormalizer.normalizePhoneNumber(model.getPersonPII().getPhone());
        model.getPersonPII().setPhoneHash(hashingService.generateHash(normalizedPhone));

        CustomerAuthDataModel customerAuth = applicationContext.getBean(CustomerAuthDataModel.class);
        customerAuth.setProvider(dto.getProvider());
        customerAuth.setToken(dto.getToken());
        model.setCustomerAuth(customerAuth);

        return model;
    }
}
