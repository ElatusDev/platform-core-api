/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.notifications.email;

import com.akademiaplus.infra.TenantScoped;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;


/**
 * Entity representing email attachments in the multi-tenant notification system.
 * Junction table that links emails to their attachment URLs,
 * supporting multiple attachments per email message.
 * <p>
 * Each attachment record is uniquely identified by the combination of
 * tenant ID, email ID, and attachment URL.
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Scope("prototype")
@Component
@Entity
@Table(name = "email_attachments")
@IdClass(EmailAttachmentDataModel.EmailAttachmentCompositeId.class)
public class EmailAttachmentDataModel extends TenantScoped {

    /**
     * Reference to the email ID this attachment belongs to.
     * Part of the composite primary key for tenant isolation.
     */
    @Id
    @Column(name = "email_id")
    private Integer emailId;

    /**
     * URL or path to the attachment file.
     * Part of the composite primary key and contains the location of the attached file.
     */
    @Id
    @Column(name = "attachment_url", columnDefinition = "TEXT")
    private String attachmentUrl;

    /**
     * Reference to the parent email entity with tenant-aware join.
     * Uses composite foreign key to maintain tenant isolation.
     */
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", referencedColumnName = "tenant_id")
    @JoinColumn(name = "email_id", referencedColumnName = "email_id", insertable = false, updatable = false)
    private EmailDataModel email;

    /**
     * Composite primary key class for EmailAttachment entity.
     * Combines tenant ID, email ID, and attachment URL for uniqueness.
     */
    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class EmailAttachmentCompositeId {

        private Integer tenantId;
        private Integer emailId;
        private String attachmentUrl;

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof EmailAttachmentCompositeId that)) return false;
            return tenantId.equals(that.tenantId) &&
                    emailId.equals(that.emailId) &&
                    attachmentUrl.equals(that.attachmentUrl);
        }

        @Override
        public int hashCode() {
            return java.util.Objects.hash(tenantId, emailId, attachmentUrl);
        }

        @Override
        public String toString() {
            return getClass().getSimpleName() +
                    "{tenantId=" + tenantId +
                    ", emailId=" + emailId +
                    ", attachmentUrl='" + attachmentUrl + "'}";
        }
    }
}