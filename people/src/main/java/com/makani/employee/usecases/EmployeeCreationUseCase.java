/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.makani.employee.usecases;

import com.makani.PersonPIIDataModel;
import com.makani.people.employee.EmployeeDataModel;
import com.makani.employee.interfaceadapters.EmployeeRepository;
import com.makani.security.user.InternalAuthDataModel;
import com.makani.utilities.security.HashingService;
import com.makani.utilities.security.PiiNormalizer;
import openapi.makani.domain.people.dto.EmployeeCreationRequestDTO;
import openapi.makani.domain.people.dto.EmployeeCreationResponseDTO;
import openapi.makani.domain.people.dto.InternalAuthDTO;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;

@Service
public class EmployeeCreationUseCase {
    private final EmployeeRepository employeeRepository;
    private final ModelMapper modelMapper;
    private final HashingService hashingService;
    private final PiiNormalizer piiNormalizer;
    private final Set<InternalAuthDataModel> set;
    public static final String MAP_NAME = "employeeMap";

    public EmployeeCreationUseCase(EmployeeRepository employeeRepository,
                                   ModelMapper modelMapper,
                                   HashingService hashingService,
                                   PiiNormalizer piiNormalizer) {
        this.employeeRepository = employeeRepository;
        this.modelMapper = modelMapper;
        this.hashingService = hashingService;
        this.piiNormalizer = piiNormalizer;
        set = new HashSet<>();
    }

    @Transactional
    public EmployeeCreationResponseDTO create(EmployeeCreationRequestDTO dto)  {
        return modelMapper.map(employeeRepository.save(transform(dto)), EmployeeCreationResponseDTO.class);
    }

    public EmployeeDataModel transform(EmployeeCreationRequestDTO dto) {
        final InternalAuthDataModel internalAuthDataModel= modelMapper.map(dto.getInternalAuth(), InternalAuthDataModel.class);
        final PersonPIIDataModel personPIIDataModel = modelMapper.map(dto, PersonPIIDataModel.class);
        final EmployeeDataModel model =  modelMapper.map(dto, EmployeeDataModel.class, MAP_NAME);
        model.setPersonPII(personPIIDataModel);
        model.setInternalAuth(internalAuthDataModel);

        String normalizedEmail = piiNormalizer.normalizeEmail(model.getPersonPII().getEmail());
        model.getPersonPII().setEmailHash(hashingService.generateHash(normalizedEmail));

        String normalizedPhone = piiNormalizer.normalizePhoneNumber(model.getPersonPII().getPhone());
        model.getPersonPII().setPhoneHash(hashingService.generateHash(normalizedPhone));

        internalAuthDataModel.setUsernameHash(hashingService.generateHash(internalAuthDataModel.getUsername()));
        return model;
    }


}
