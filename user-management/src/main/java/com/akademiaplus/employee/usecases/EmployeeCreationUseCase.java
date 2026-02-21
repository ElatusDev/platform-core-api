/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.employee.usecases;

import com.akademiaplus.users.base.PersonPIIDataModel;
import com.akademiaplus.users.employee.EmployeeDataModel;
import com.akademiaplus.employee.interfaceadapters.EmployeeRepository;
import com.akademiaplus.security.InternalAuthDataModel;
import com.akademiaplus.utilities.security.HashingService;
import com.akademiaplus.utilities.security.PiiNormalizer;
import lombok.RequiredArgsConstructor;
import openapi.akademiaplus.domain.user.management.dto.EmployeeCreationRequestDTO;
import openapi.akademiaplus.domain.user.management.dto.EmployeeCreationResponseDTO;
import org.modelmapper.ModelMapper;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
public class EmployeeCreationUseCase {
    public static final String MAP_NAME = "employeeMap";

    private final ApplicationContext applicationContext;
    private final ModelMapper modelMapper;
    private final HashingService hashingService;
    private final PiiNormalizer piiNormalizer;
    private final EmployeeRepository employeeRepository;

    @Transactional
    public EmployeeCreationResponseDTO create(EmployeeCreationRequestDTO dto)  {
        return modelMapper.map(employeeRepository.saveAndFlush(transform(dto)), EmployeeCreationResponseDTO.class);
    }

    public EmployeeDataModel transform(EmployeeCreationRequestDTO dto) {
        final InternalAuthDataModel internalAuthDataModel = applicationContext.getBean(InternalAuthDataModel.class);
        modelMapper.map(dto, internalAuthDataModel);

        final PersonPIIDataModel personPIIDataModel = applicationContext.getBean(PersonPIIDataModel.class);
        modelMapper.map(dto, personPIIDataModel);
        personPIIDataModel.setPhoneNumber(dto.getPhoneNumber());

        final EmployeeDataModel model = applicationContext.getBean(EmployeeDataModel.class);
        modelMapper.map(dto, model, MAP_NAME);
        model.setPersonPII(personPIIDataModel);
        model.setInternalAuth(internalAuthDataModel);

        String normalizedEmail = piiNormalizer.normalizeEmail(model.getPersonPII().getEmail());
        model.getPersonPII().setEmailHash(hashingService.generateHash(normalizedEmail));

        String normalizedPhone = piiNormalizer.normalizePhoneNumber(model.getPersonPII().getPhoneNumber());
        model.getPersonPII().setPhoneHash(hashingService.generateHash(normalizedPhone));

        internalAuthDataModel.setUsernameHash(hashingService.generateHash(internalAuthDataModel.getUsername()));
        return model;
    }



}
