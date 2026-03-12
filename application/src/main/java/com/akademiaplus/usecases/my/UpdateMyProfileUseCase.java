/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.usecases.my;

import com.akademiaplus.customer.adultstudent.interfaceadapters.AdultStudentRepository;
import com.akademiaplus.customer.interfaceadapters.TutorRepository;
import com.akademiaplus.infra.persistence.config.TenantContextHolder;
import com.akademiaplus.internal.interfaceadapters.UserContextHolder;
import com.akademiaplus.internal.interfaceadapters.jwt.JwtTokenProvider;
import com.akademiaplus.users.base.AbstractUser;
import com.akademiaplus.users.base.PersonPIIDataModel;
import com.akademiaplus.utilities.EntityType;
import com.akademiaplus.utilities.exceptions.EntityNotFoundException;
import com.akademiaplus.utilities.security.HashingService;
import com.akademiaplus.utilities.security.PiiNormalizer;
import openapi.akademiaplus.domain.my.dto.MyProfileResponseDTO;
import openapi.akademiaplus.domain.my.dto.UpdateMyProfileRequestDTO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Updates the authenticated customer's own profile.
 *
 * @author ElatusDev
 * @since 1.0
 */
@Service
public class UpdateMyProfileUseCase {

    /** Error message for unsupported profile type. */
    public static final String ERROR_UNSUPPORTED_PROFILE_TYPE = "Unsupported profile type";

    private final UserContextHolder userContextHolder;
    private final TenantContextHolder tenantContextHolder;
    private final AdultStudentRepository adultStudentRepository;
    private final TutorRepository tutorRepository;
    private final PiiNormalizer piiNormalizer;
    private final HashingService hashingService;

    /**
     * Constructs the use case with required dependencies.
     *
     * @param userContextHolder      the user context holder
     * @param tenantContextHolder    the tenant context holder
     * @param adultStudentRepository the adult student repository
     * @param tutorRepository        the tutor repository
     * @param piiNormalizer          the PII normalizer
     * @param hashingService         the hashing service
     */
    public UpdateMyProfileUseCase(UserContextHolder userContextHolder,
                                   TenantContextHolder tenantContextHolder,
                                   AdultStudentRepository adultStudentRepository,
                                   TutorRepository tutorRepository,
                                   PiiNormalizer piiNormalizer,
                                   HashingService hashingService) {
        this.userContextHolder = userContextHolder;
        this.tenantContextHolder = tenantContextHolder;
        this.adultStudentRepository = adultStudentRepository;
        this.tutorRepository = tutorRepository;
        this.piiNormalizer = piiNormalizer;
        this.hashingService = hashingService;
    }

    /**
     * Updates the authenticated customer's profile with the provided fields.
     *
     * @param request the update request DTO
     * @return the updated profile response DTO
     * @throws EntityNotFoundException if the profile entity does not exist
     */
    @Transactional
    public MyProfileResponseDTO execute(UpdateMyProfileRequestDTO request) {
        String profileType = userContextHolder.requireProfileType();
        Long profileId = userContextHolder.requireProfileId();
        Long tenantId = tenantContextHolder.requireTenantId();

        AbstractUser user = resolveUser(profileType, profileId, tenantId);
        applyUpdates(user.getPersonPII(), request);

        return mapToDto(user, profileType, profileId);
    }

    private AbstractUser resolveUser(String profileType, Long profileId, Long tenantId) {
        if (JwtTokenProvider.PROFILE_TYPE_ADULT_STUDENT.equals(profileType)) {
            return adultStudentRepository.findById(
                    new com.akademiaplus.users.customer.AdultStudentDataModel.AdultStudentCompositeId(tenantId, profileId))
                    .orElseThrow(() -> new EntityNotFoundException(EntityType.ADULT_STUDENT, profileId.toString()));
        }
        if (JwtTokenProvider.PROFILE_TYPE_TUTOR.equals(profileType)) {
            return tutorRepository.findById(
                    new com.akademiaplus.users.customer.TutorDataModel.TutorCompositeId(tenantId, profileId))
                    .orElseThrow(() -> new EntityNotFoundException(EntityType.TUTOR, profileId.toString()));
        }
        throw new EntityNotFoundException(ERROR_UNSUPPORTED_PROFILE_TYPE, profileType);
    }

    private void applyUpdates(PersonPIIDataModel pii, UpdateMyProfileRequestDTO request) {
        if (request.getFirstName() != null) {
            pii.setFirstName(request.getFirstName());
        }
        if (request.getLastName() != null) {
            pii.setLastName(request.getLastName());
        }
        if (request.getPhoneNumber() != null) {
            String normalizedPhone = piiNormalizer.normalizePhoneNumber(request.getPhoneNumber());
            pii.setPhoneNumber(normalizedPhone);
            pii.setPhoneHash(hashingService.generateHash(normalizedPhone));
        }
        if (request.getAddress() != null) {
            pii.setAddress(request.getAddress());
        }
        if (request.getZipCode() != null) {
            pii.setZipCode(request.getZipCode());
        }
    }

    private MyProfileResponseDTO mapToDto(AbstractUser user, String profileType, Long profileId) {
        MyProfileResponseDTO dto = new MyProfileResponseDTO();
        dto.setProfileId(profileId);
        dto.setProfileType(MyProfileResponseDTO.ProfileTypeEnum.fromValue(profileType));
        dto.setFirstName(user.getPersonPII().getFirstName());
        dto.setLastName(user.getPersonPII().getLastName());
        dto.setEmail(user.getPersonPII().getEmail());
        dto.setPhoneNumber(user.getPersonPII().getPhoneNumber());
        dto.setAddress(user.getPersonPII().getAddress());
        dto.setZipCode(user.getPersonPII().getZipCode());
        dto.setBirthDate(user.getBirthDate());
        dto.setEntryDate(user.getEntryDate());
        return dto;
    }
}
