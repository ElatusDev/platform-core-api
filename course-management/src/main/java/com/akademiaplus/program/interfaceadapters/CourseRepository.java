/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.program.interfaceadapters;

import com.akademiaplus.courses.program.CourseDataModel;
import com.akademiaplus.utilities.persistence.repository.TenantScopedRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CourseRepository extends TenantScopedRepository<CourseDataModel, CourseDataModel.CourseCompositeId> {

    /**
     * Finds courses where the given collaborator is listed as available.
     *
     * @param collaboratorId the collaborator ID
     * @return courses the collaborator is available to teach
     */
    @Query("SELECT c FROM CourseDataModel c JOIN c.availableCollaborators ac "
         + "WHERE ac.collaboratorId = :collaboratorId")
    List<CourseDataModel> findByAvailableCollaboratorId(@Param("collaboratorId") Long collaboratorId);
}
