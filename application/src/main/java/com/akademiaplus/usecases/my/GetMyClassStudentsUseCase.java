/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.usecases.my;

import com.akademiaplus.courses.event.CourseEventDataModel;
import com.akademiaplus.event.interfaceadapters.CourseEventRepository;
import com.akademiaplus.infra.persistence.config.TenantContextHolder;
import com.akademiaplus.internal.interfaceadapters.UserContextHolder;
import com.akademiaplus.internal.interfaceadapters.jwt.JwtTokenProvider;
import com.akademiaplus.utilities.EntityType;
import com.akademiaplus.utilities.exceptions.EntityNotFoundException;
import openapi.akademiaplus.domain.my.dto.MyClassStudentDTO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * Retrieves students attending a specific class taught by the authenticated collaborator.
 *
 * <p>Enforces ownership: the class must be assigned to the requesting collaborator.
 * Returns 404 if the class does not exist or belongs to another collaborator.</p>
 *
 * @author ElatusDev
 * @since 1.0
 */
@Service
public class GetMyClassStudentsUseCase {

    /** Error message when profile type is not COLLABORATOR. */
    public static final String ERROR_NOT_COLLABORATOR = "Only collaborators can access class students";

    /** Student type value for adult students. */
    public static final String STUDENT_TYPE_ADULT = "ADULT";

    /** Student type value for minor students. */
    public static final String STUDENT_TYPE_MINOR = "MINOR";

    private final UserContextHolder userContextHolder;
    private final TenantContextHolder tenantContextHolder;
    private final CourseEventRepository courseEventRepository;

    /**
     * Constructs the use case with required dependencies.
     *
     * @param userContextHolder      the user context holder
     * @param tenantContextHolder    the tenant context holder
     * @param courseEventRepository   the course event repository
     */
    public GetMyClassStudentsUseCase(UserContextHolder userContextHolder,
                                      TenantContextHolder tenantContextHolder,
                                      CourseEventRepository courseEventRepository) {
        this.userContextHolder = userContextHolder;
        this.tenantContextHolder = tenantContextHolder;
        this.courseEventRepository = courseEventRepository;
    }

    /**
     * Returns students attending the specified class, verifying the collaborator owns it.
     *
     * @param classId the course event ID
     * @return list of student DTOs
     * @throws IllegalStateException   if the profile type is not COLLABORATOR
     * @throws EntityNotFoundException if the class does not exist or is not assigned to this collaborator
     */
    @Transactional(readOnly = true)
    public List<MyClassStudentDTO> execute(Long classId) {
        String profileType = userContextHolder.requireProfileType();
        if (!JwtTokenProvider.PROFILE_TYPE_COLLABORATOR.equals(profileType)) {
            throw new IllegalStateException(ERROR_NOT_COLLABORATOR);
        }
        Long profileId = userContextHolder.requireProfileId();
        Long tenantId = tenantContextHolder.requireTenantId();

        CourseEventDataModel event = courseEventRepository
                .findById(new CourseEventDataModel.CourseEventCompositeId(tenantId, classId))
                .filter(ce -> ce.getCollaboratorId().equals(profileId))
                .orElseThrow(() -> new EntityNotFoundException(EntityType.COURSE_EVENT, classId.toString()));

        List<MyClassStudentDTO> students = new ArrayList<>();

        if (event.getAdultAttendees() != null) {
            event.getAdultAttendees().forEach(adult -> {
                MyClassStudentDTO dto = new MyClassStudentDTO();
                dto.setStudentId(adult.getAdultStudentId());
                dto.setFirstName(adult.getPersonPII().getFirstName());
                dto.setLastName(adult.getPersonPII().getLastName());
                dto.setStudentType(MyClassStudentDTO.StudentTypeEnum.ADULT);
                students.add(dto);
            });
        }

        if (event.getMinorAttendees() != null) {
            event.getMinorAttendees().forEach(minor -> {
                MyClassStudentDTO dto = new MyClassStudentDTO();
                dto.setStudentId(minor.getMinorStudentId());
                dto.setFirstName(minor.getPersonPII().getFirstName());
                dto.setLastName(minor.getPersonPII().getLastName());
                dto.setStudentType(MyClassStudentDTO.StudentTypeEnum.MINOR);
                students.add(dto);
            });
        }

        return students;
    }
}
