/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.notification.interfaceadapters;

import com.akademiaplus.notifications.email.EmailAttachmentDataModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * JPA repository for {@link EmailAttachmentDataModel} entities.
 */
@Repository
public interface EmailAttachmentRepository extends JpaRepository<EmailAttachmentDataModel, Long> {
}
