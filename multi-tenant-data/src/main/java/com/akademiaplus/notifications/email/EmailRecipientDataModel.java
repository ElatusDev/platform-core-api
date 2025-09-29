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
 * Entity representing email recipients in the multi-tenant notification system.
 * Junction table that links emails to their recipient email addresses,
 * supporting multiple recipients per email message.
 * <p>
 * Each recipient record is uniquely identified by the combination of
 * tenant ID, email ID, and recipient email address.
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Scope("prototype")
@Component
@Entity
@Table(name = "email_recipients")
@IdClass(EmailRecipientDataModel.EmailRecipientCompositeId.class)
public class EmailRecipientDataModel extends TenantScoped {

    /**
     * Reference to the email ID this recipient received.
     * Part of the composite primary key for tenant isolation.
     */
    @Id
    @Column(name = "email_id")
    private Integer emailId;

    /**
     * Email address of the recipient.
     * Part of the composite primary key and contains the actual recipient email.
     */
    @Id
    @Column(name = "recipient_email", length = 150)
    private String recipientEmail;

    /**
     * Reference to the parent email entity with tenant-aware join.
     * Uses composite foreign key to maintain tenant isolation.
     */
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", referencedColumnName = "tenant_id")
    @JoinColumn(name = "email_id", referencedColumnName = "email_id", insertable = false, updatable = false)
    private EmailDataModel email;

    /**
     * Composite primary key class for EmailRecipient entity.
     * Combines tenant ID, email ID, and recipient email for uniqueness.
     */
    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class EmailRecipientCompositeId {

        private Integer tenantId;
        private Integer emailId;
        private String recipientEmail;

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof EmailRecipientCompositeId that)) return false;
            return tenantId.equals(that.tenantId) &&
                    emailId.equals(that.emailId) &&
                    recipientEmail.equals(that.recipientEmail);
        }

        @Override
        public int hashCode() {
            return java.util.Objects.hash(tenantId, emailId, recipientEmail);
        }

        @Override
        public String toString() {
            return getClass().getSimpleName() +
                    "{tenantId=" + tenantId +
                    ", emailId=" + emailId +
                    ", recipientEmail='" + recipientEmail + "'}";
        }
    }
}
