/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.notifications.email;

 import com.akademiaplus.infra.persistence.model.TenantScoped;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Entity representing emails in the multi-tenant notification system.
 * Stores email content, metadata, and sender information for emails
 * sent through the platform's notification service.
 * <p>
 * Each email is uniquely identified within a tenant and can have
 * multiple recipients and attachments through related entities.
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Scope("prototype")
@Component
@Entity
@Table(name = "emails")
@SQLDelete(sql = "UPDATE emails SET deleted_at = CURRENT_TIMESTAMP WHERE tenant_id = ?")
@IdClass(EmailDataModel.EmailCompositeId.class)
public class EmailDataModel extends TenantScoped {

    /**
     * Unique identifier for the email within the tenant.
     * Auto-incremented per tenant for better performance and serves as part of the composite key.
     */
    @Id
    @Column(name = "email_id")
    private Long emailId;

    /**
     * Subject line of the email.
     * Contains the email's topic or title that recipients will see.
     */
    @Column(name = "subject", nullable = false, length = 255)
    private String subject;

    /**
     * Main content body of the email.
     * Contains the complete message text, may include HTML formatting.
     */
    @Column(name = "body", nullable = false, columnDefinition = "TEXT")
    private String body;

    /**
     * Email address or identifier of the email sender.
     * Used for tracking email origins and reply-to information.
     */
    @Column(name = "sender", nullable = false, length = 150)
    private String sender;

    /**
     * List of recipients who received this email.
     * Uses tenant-aware mapping to maintain data isolation.
     */
    @OneToMany(mappedBy = "email", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private List<EmailRecipientDataModel> recipients;

    /**
     * List of attachments included with this email.
     * Uses tenant-aware mapping to maintain data isolation.
     */
    @OneToMany(mappedBy = "email", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private List<EmailAttachmentDataModel> attachments;

    /**
     * Composite primary key class for Email entity.
     * Combines tenant ID and email ID for uniqueness across tenants.
     */
    @Data
    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class EmailCompositeId {
        private Long tenantId;
        private Long emailId;
    }
}
