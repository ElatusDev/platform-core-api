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
import com.akademiaplus.users.customer.TutorDataModel;
import com.akademiaplus.utilities.EntityType;
import com.akademiaplus.utilities.exceptions.EntityDeletionNotAllowedException;
import com.akademiaplus.utilities.exceptions.EntityNotFoundException;
import com.akademiaplus.utilities.usecases.DeleteUseCaseSupport;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Handles soft-deletion of a {@link TutorDataModel}.
 * <p>
 * Business rule: a Tutor with active (non-deleted) MinorStudents
 * cannot be deleted. The pre-delete check prevents orphaned students.
 *
 * @author ElatusDev
 * @since 1.0
 */
@Service
public class DeleteTutorUseCase {

    /** Reason message when tutor has active minor students. */
    public static final String ACTIVE_MINOR_STUDENTS_REASON =
            "Tutor tiene %d alumno(s) menor(es) activo(s)";

    private final TutorRepository tutorRepository;
    private final MinorStudentRepository minorStudentRepository;
    private final TenantContextHolder tenantContextHolder;

    public DeleteTutorUseCase(TutorRepository tutorRepository,
                              MinorStudentRepository minorStudentRepository,
                              TenantContextHolder tenantContextHolder) {
        this.tutorRepository = tutorRepository;
        this.minorStudentRepository = minorStudentRepository;
        this.tenantContextHolder = tenantContextHolder;
    }

    /**
     * Soft-deletes a Tutor after verifying they have no active MinorStudents.
     *
     * @param tutorId the tutor's entity-specific ID
     * @throws EntityNotFoundException if no tutor exists with the given ID
     * @throws EntityDeletionNotAllowedException if tutor has active minor students
     *         (business rule) or a DB constraint prevents deletion
     * @throws com.akademiaplus.utilities.exceptions.InvalidTenantException
     *         if no tenant context is set
     */
    @Transactional
    public void delete(Long tutorId) {
        Long tenantId = tenantContextHolder.requireTenantId();
        String entityId = String.valueOf(tutorId);

        TutorDataModel tutor = DeleteUseCaseSupport.findOrThrow(
                tutorRepository,
                new TutorDataModel.TutorCompositeId(tenantId, tutorId),
                EntityType.TUTOR,
                entityId);

        long activeMinorStudents = minorStudentRepository
                .countByTenantIdAndTutorId(tenantId, tutorId);
        if (activeMinorStudents > 0) {
            throw new EntityDeletionNotAllowedException(
                    EntityType.TUTOR,
                    entityId,
                    String.format(ACTIVE_MINOR_STUDENTS_REASON, activeMinorStudents));
        }

        try {
            tutorRepository.delete(tutor);
        } catch (DataIntegrityViolationException ex) {
            throw new EntityDeletionNotAllowedException(
                    EntityType.TUTOR, entityId, ex);
        }
    }
}
