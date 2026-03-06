/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.notification.interfaceadapters;

import com.akademiaplus.notifications.email.EmailTemplateDataModel;
import com.akademiaplus.utilities.persistence.repository.TenantScopedRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * JPA repository for {@link EmailTemplateDataModel} entities.
 */
@Repository
public interface EmailTemplateRepository extends TenantScopedRepository<EmailTemplateDataModel, EmailTemplateDataModel.EmailTemplateCompositeId> {

    /**
     * Finds a template by its template ID within the current tenant.
     *
     * @param templateId the template identifier
     * @return optional template
     */
    Optional<EmailTemplateDataModel> findByTemplateId(Long templateId);

    /**
     * Finds all templates matching the given category within the current tenant.
     *
     * @param category the template category
     * @return list of matching templates
     */
    List<EmailTemplateDataModel> findByCategory(String category);
}
