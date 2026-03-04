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
import com.akademiaplus.infra.persistence.config.TenantContextHolder;
import com.akademiaplus.interfaceadapters.PersonPIIRepository;
import com.akademiaplus.security.CustomerAuthDataModel;
import com.akademiaplus.users.base.PersonPIIDataModel;
import com.akademiaplus.users.customer.MinorStudentDataModel;
import com.akademiaplus.users.customer.TutorDataModel;
import com.akademiaplus.utilities.EntityType;
import com.akademiaplus.utilities.PiiField;
import com.akademiaplus.utilities.exceptions.DuplicateEntityException;
import com.akademiaplus.utilities.security.HashingService;
import com.akademiaplus.utilities.security.PiiNormalizer;
import lombok.RequiredArgsConstructor;
import openapi.akademiaplus.domain.user.management.dto.MinorStudentCreationRequestDTO;
import openapi.akademiaplus.domain.user.management.dto.MinorStudentCreationResponseDTO;
import openapi.akademiaplus.domain.user.management.dto.TutorCreationRequestDTO;
import openapi.akademiaplus.domain.user.management.dto.TutorCreationResponseDTO;
import org.modelmapper.ModelMapper;
import org.springframework.context.ApplicationContext;
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
 * <p>
 * Uses named TypeMaps to prevent ModelMapper deep-matching pollution.
 * Without a named TypeMap, {@code MinorStudentCreationRequestDTO.tutorId}
 * deep-matches into both {@code minorStudentId} and {@code tutor.tutorId},
 * creating a phantom detached {@link TutorDataModel}.
 */
@RequiredArgsConstructor
@Service
public class TutorCreationUseCase {
    public static final String TUTOR_MAP_NAME = "tutorMap";
    public static final String MINOR_STUDENT_MAP_NAME = "minorStudentMap";

    private final ApplicationContext applicationContext;
    private final TutorRepository tutorRepository;
    private final MinorStudentRepository minorStudentRepository;
    private final PersonPIIRepository personPIIRepository;
    private final TenantContextHolder tenantContextHolder;
    private final ModelMapper modelMapper;
    private final HashingService hashingService;
    private final PiiNormalizer piiNormalizer;

    @Transactional
    public TutorCreationResponseDTO create(TutorCreationRequestDTO dto) {
        TutorDataModel model = transformTutor(dto);
        PersonPIIDataModel pii = model.getPersonPII();
        if (personPIIRepository.existsByEmailHash(pii.getEmailHash())) {
            throw new DuplicateEntityException(EntityType.TUTOR, PiiField.EMAIL);
        }
        if (personPIIRepository.existsByPhoneHash(pii.getPhoneHash())) {
            throw new DuplicateEntityException(EntityType.TUTOR, PiiField.PHONE_NUMBER);
        }
        return modelMapper.map(tutorRepository.saveAndFlush(model), TutorCreationResponseDTO.class);
    }

    @Transactional
    public MinorStudentCreationResponseDTO createMinorStudent(MinorStudentCreationRequestDTO dto) {
        MinorStudentDataModel model = transformMinorStudent(dto);
        PersonPIIDataModel pii = model.getPersonPII();
        if (personPIIRepository.existsByEmailHash(pii.getEmailHash())) {
            throw new DuplicateEntityException(EntityType.MINOR_STUDENT, PiiField.EMAIL);
        }
        if (personPIIRepository.existsByPhoneHash(pii.getPhoneHash())) {
            throw new DuplicateEntityException(EntityType.MINOR_STUDENT, PiiField.PHONE_NUMBER);
        }
        return modelMapper.map(minorStudentRepository.saveAndFlush(model), MinorStudentCreationResponseDTO.class);
    }

    /**
     * Transforms a Tutor creation DTO into a persistence-ready data model.
     * <p>
     * Uses a named TypeMap ({@value TUTOR_MAP_NAME}) to prevent deep-matching
     * into nested {@code personPII} and {@code customerAuth} objects.
     * PersonPII is mapped in a separate call; CustomerAuth is built manually
     * because provider/token are {@code JsonNullable} in the DTO.
     *
     * @param dto the tutor creation request
     * @return populated TutorDataModel ready for persistence
     */
    public TutorDataModel transformTutor(TutorCreationRequestDTO dto) {
        final PersonPIIDataModel personPII = applicationContext.getBean(PersonPIIDataModel.class);
        modelMapper.map(dto, personPII);
        personPII.setPhoneNumber(dto.getPhoneNumber());

        final TutorDataModel model = applicationContext.getBean(TutorDataModel.class);
        modelMapper.map(dto, model, TUTOR_MAP_NAME);
        model.setPersonPII(personPII);
        model.setEntryDate(LocalDate.now());

        hashPii(personPII);

        if (dto.getProvider() != null && dto.getProvider().isPresent()) {
            CustomerAuthDataModel customerAuth = applicationContext.getBean(CustomerAuthDataModel.class);
            customerAuth.setProvider(dto.getProvider().get());
            customerAuth.setToken(dto.getToken().get());
            model.setCustomerAuth(customerAuth);
        }

        return model;
    }

    /**
     * Transforms a MinorStudent creation DTO into a persistence-ready data model.
     * <p>
     * Uses a named TypeMap ({@value MINOR_STUDENT_MAP_NAME}) to prevent
     * {@code dto.tutorId} from deep-matching into {@code minorStudentId} and
     * {@code tutor.tutorId}. The tutor relationship is resolved via repository
     * lookup, not through ModelMapper.
     *
     * @param dto the minor student creation request
     * @return populated MinorStudentDataModel ready for persistence
     * @throws IllegalArgumentException if the referenced tutor does not exist
     */
    public MinorStudentDataModel transformMinorStudent(MinorStudentCreationRequestDTO dto) {
        final PersonPIIDataModel personPII = applicationContext.getBean(PersonPIIDataModel.class);
        modelMapper.map(dto, personPII);
        personPII.setPhoneNumber(dto.getPhoneNumber());

        final MinorStudentDataModel model = applicationContext.getBean(MinorStudentDataModel.class);
        modelMapper.map(dto, model, MINOR_STUDENT_MAP_NAME);
        model.setPersonPII(personPII);
        model.setEntryDate(LocalDate.now());

        hashPii(personPII);

        CustomerAuthDataModel customerAuth = applicationContext.getBean(CustomerAuthDataModel.class);
        customerAuth.setProvider(dto.getProvider());
        customerAuth.setToken(dto.getToken());
        model.setCustomerAuth(customerAuth);

        Long tenantId = tenantContextHolder.getTenantId()
                .orElseThrow(() -> new IllegalArgumentException("Tenant context is required"));
        TutorDataModel tutor = tutorRepository.findById(
                        new TutorDataModel.TutorCompositeId(tenantId, dto.getTutorId()))
                .orElseThrow(() -> new IllegalArgumentException("Tutor not found: " + dto.getTutorId()));
        model.setTutor(tutor);

        return model;
    }

    private void hashPii(PersonPIIDataModel personPII) {
        String normalizedEmail = piiNormalizer.normalizeEmail(personPII.getEmail());
        personPII.setEmailHash(hashingService.generateHash(normalizedEmail));

        String normalizedPhone = piiNormalizer.normalizePhoneNumber(personPII.getPhoneNumber());
        personPII.setPhoneHash(hashingService.generateHash(normalizedPhone));
    }
}
