/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.customer.tutor.usecases;

import com.akademiaplus.customer.interfaceadapters.TutorRepository;
import com.akademiaplus.customer.minorstudent.interfaceadapters.MinorStudentRepository;
import com.akademiaplus.security.CustomerAuthDataModel;
import com.akademiaplus.users.base.PersonPIIDataModel;
import com.akademiaplus.users.customer.MinorStudentDataModel;
import com.akademiaplus.users.customer.TutorDataModel;
import com.akademiaplus.utilities.security.HashingService;
import com.akademiaplus.utilities.security.PiiNormalizer;
import lombok.RequiredArgsConstructor;
import openapi.akademiaplus.domain.user.management.dto.MinorStudentCreationRequestDTO;
import openapi.akademiaplus.domain.user.management.dto.MinorStudentCreationResponseDTO;
import openapi.akademiaplus.domain.user.management.dto.TutorCreationRequestDTO;
import openapi.akademiaplus.domain.user.management.dto.TutorCreationResponseDTO;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

/**
 * Use case responsible for creating Tutors and Minor Students.
 * <p>
 * This use case owns both entity lifecycles because a Minor Student
 * can never exist without a Tutor. A Tutor may be created alone,
 * but a Minor Student must always be created from an existing Tutor
 * or together with a new Tutor — never independently.
 */
@RequiredArgsConstructor
@Service
public class TutorCreationUseCase {

    private final TutorRepository tutorRepository;
    private final MinorStudentRepository minorStudentRepository;
    private final ModelMapper modelMapper;
    private final HashingService hashingService;
    private final PiiNormalizer piiNormalizer;

    @Transactional
    public TutorCreationResponseDTO create(TutorCreationRequestDTO dto) {
        return modelMapper.map(tutorRepository.save(transformTutor(dto)), TutorCreationResponseDTO.class);
    }

    @Transactional
    public MinorStudentCreationResponseDTO createMinorStudent(MinorStudentCreationRequestDTO dto) {
        return modelMapper.map(minorStudentRepository.save(transformMinorStudent(dto)), MinorStudentCreationResponseDTO.class);
    }

    public TutorDataModel transformTutor(TutorCreationRequestDTO dto) {
        TutorDataModel model = modelMapper.map(dto, TutorDataModel.class);

        final PersonPIIDataModel personPII = modelMapper.map(dto, PersonPIIDataModel.class);
        model.setPersonPII(personPII);
        model.setEntryDate(LocalDate.now());

        hashPii(personPII);

        if (dto.getProvider() != null && dto.getProvider().isPresent()) {
            CustomerAuthDataModel customerAuth = new CustomerAuthDataModel();
            customerAuth.setProvider(dto.getProvider().get());
            customerAuth.setToken(dto.getToken().get());
            model.setCustomerAuth(customerAuth);
        }

        return model;
    }

    public MinorStudentDataModel transformMinorStudent(MinorStudentCreationRequestDTO dto) {
        MinorStudentDataModel model = modelMapper.map(dto, MinorStudentDataModel.class);

        final PersonPIIDataModel personPII = modelMapper.map(dto, PersonPIIDataModel.class);
        model.setPersonPII(personPII);
        model.setEntryDate(LocalDate.now());

        hashPii(personPII);

        CustomerAuthDataModel customerAuth = new CustomerAuthDataModel();
        customerAuth.setProvider(dto.getProvider());
        customerAuth.setToken(dto.getToken());
        model.setCustomerAuth(customerAuth);

        TutorDataModel tutor = tutorRepository.findById(dto.getTutorId())
                .orElseThrow(() -> new IllegalArgumentException("Tutor not found: " + dto.getTutorId()));
        model.setTutor(tutor);

        return model;
    }

    private void hashPii(PersonPIIDataModel personPII) {
        String normalizedEmail = piiNormalizer.normalizeEmail(personPII.getEmail());
        personPII.setEmailHash(hashingService.generateHash(normalizedEmail));

        String normalizedPhone = piiNormalizer.normalizePhoneNumber(personPII.getPhone());
        personPII.setPhoneHash(hashingService.generateHash(normalizedPhone));
    }
}
