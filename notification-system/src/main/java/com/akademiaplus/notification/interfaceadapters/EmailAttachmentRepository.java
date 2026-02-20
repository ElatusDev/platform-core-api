/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.notification.interfaceadapters;

import com.akademiaplus.utilities.persistence.repository.TenantScopedRepository;
import com.akademiaplus.notifications.email.EmailAttachmentDataModel;
import org.springframework.stereotype.Repository;

/**
 * JPA repository for {@link EmailAttachmentDataModel} entities.
 */
@Repository
public interface EmailAttachmentRepository extends TenantScopedRepository<EmailAttachmentDataModel, EmailAttachmentDataModel.EmailAttachmentCompositeId> {
}
