/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.notification.interfaceadapters;

import com.akademiaplus.notifications.email.EmailTemplateVariableDataModel;
import com.akademiaplus.utilities.persistence.repository.TenantScopedRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * JPA repository for {@link EmailTemplateVariableDataModel} entities.
 */
@Repository
public interface EmailTemplateVariableRepository extends TenantScopedRepository<EmailTemplateVariableDataModel, EmailTemplateVariableDataModel.EmailTemplateVariableCompositeId> {

    /**
     * Finds all variables associated with a given template.
     *
     * @param templateId the parent template identifier
     * @return list of template variables
     */
    List<EmailTemplateVariableDataModel> findByTemplateId(Long templateId);
}
