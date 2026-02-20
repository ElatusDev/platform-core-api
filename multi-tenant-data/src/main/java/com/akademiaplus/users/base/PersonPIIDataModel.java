/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.users.base;

 import com.akademiaplus.infra.persistence.model.TenantScoped;
import com.akademiaplus.utilities.security.StringEncryptor;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.io.Serializable;

/**
 * Entity representing personally identifiable information (PII) for platform users.
 * This entity stores encrypted personal data with tenant isolation and comprehensive audit tracking.
 * All PII fields are encrypted at rest for security compliance.
 * <p>
 * Hash fields are used for indexing and uniqueness checks without requiring decryption,
 * providing both security and performance optimization for user lookups.
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Scope("prototype")
@Component
@Entity
@Table(name = "person_piis")
@SQLDelete(sql = "UPDATE person_piis SET deleted_at = CURRENT_TIMESTAMP WHERE tenant_id = ? AND person_pii_id = ?")
@IdClass(PersonPIIDataModel.PersonPIICompositeId.class)
public class PersonPIIDataModel extends TenantScoped {

    /**
     * Unique identifier for the person's PII within the tenant.
     * Auto-incremented per tenant for better performance than UUIDs.
     */
    @Id
    @Column(name = "person_pii_id")
    private Long personPiiId;

    /**
     * Encrypted first name of the person.
     * Automatically encrypted/decrypted using StringEncryptor.
     * <p>
     * Used for display purposes and personalization within the platform.
     */
    @Convert(converter = StringEncryptor.class)
    @Column(name = "encrypted_first_name", nullable = false, length = 500)
    private String firstName;

    /**
     * Encrypted last name of the person.
     * Automatically encrypted/decrypted using StringEncryptor.
     * <p>
     * Used for display purposes and formal identification within the platform.
     */
    @Convert(converter = StringEncryptor.class)
    @Column(name = "encrypted_last_name", nullable = false, length = 500)
    private String lastName;

    /**
     * Encrypted email address of the person.
     * Automatically encrypted/decrypted using StringEncryptor.
     * <p>
     * Primary contact method for platform communications and notifications.
     */
    @Convert(converter = StringEncryptor.class)
    @Column(name = "encrypted_email", nullable = false, length = 500)
    private String email;

    /**
     * Encrypted phone number of the person.
     * Automatically encrypted/decrypted using StringEncryptor.
     * <p>
     * Secondary contact method for urgent communications and verification.
     */
    @Convert(converter = StringEncryptor.class)
    @Column(name = "encrypted_phone_number", nullable = false, length = 500)
    private String phone;

    /**
     * Encrypted address of the person.
     * Automatically encrypted/decrypted using StringEncryptor.
     * <p>
     * Used for billing, shipping, and legal compliance requirements.
     */
    @Convert(converter = StringEncryptor.class)
    @Column(name = "encrypted_address", nullable = false, length = 500)
    private String address;

    /**
     * Encrypted zip code of the person.
     * Automatically encrypted/decrypted using StringEncryptor.
     * <p>
     * Used for location-based services and compliance requirements.
     */
    @Convert(converter = StringEncryptor.class)
    @Column(name = "encrypted_zip_code", nullable = false, length = 500)
    private String zipCode;

    /**
     * Hash of the email address for indexing and uniqueness checks.
     * Allows searching and ensuring uniqueness without decrypting the actual email.
     * <p>
     * Used in database constraints and user lookup operations for performance.
     */
    @Column(name = "email_hash", length = 64, nullable = false)
    private String emailHash;

    /**
     * Hash of the phone number for indexing and uniqueness checks.
     * Allows searching and ensuring uniqueness without decrypting the actual phone number.
     * <p>
     * Used in database constraints and user lookup operations for performance.
     */
    @Column(name = "phone_number_hash", length = 64, nullable = false)
    private String phoneHash;

    /**
     * Composite primary key class for PersonPII entity.
     */
    @Data
    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class PersonPIICompositeId implements Serializable {
        protected Long tenantId;
        protected Long personPiiId;
    }
}