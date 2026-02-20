/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.users.base;

 import com.akademiaplus.infra.persistence.model.TenantScoped;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

/**
 * Abstract base class for all user types in the platform.
 * Provides common user attributes and relationships with personal information.
 * All platform users (employees, students, tutors, collaborators) extend this class.
 * <p>
 * This class handles:
 * - Personal information relationship via PersonPII
 * - Common user attributes (birthdate, entry date, profile picture)
 * - Inherits multi-tenant isolation, audit tracking, and soft delete from TenantScoped
 */
@Getter
@Setter
@MappedSuperclass
public abstract class AbstractUser extends TenantScoped {

    /**
     * Foreign key to the user's personal information record.
     * Writable column used to persist the FK value during INSERT.
     */
    @Column(name = "person_pii_id")
    private Long personPiiId;

    /**
     * Reference to the user's personal and private information.
     * Uses composite foreign key to maintain tenant isolation.
     * All user types require personal information for identification and contact.
     */
    @OneToOne(optional = false, cascade = CascadeType.PERSIST, orphanRemoval = true)
    @JoinColumn(name = "tenant_id", referencedColumnName = "tenant_id", insertable=false, updatable=false)
    @JoinColumn(name = "person_pii_id", referencedColumnName = "person_pii_id", insertable=false, updatable=false)
    private PersonPIIDataModel personPII;

    /**
     * User's date of birth.
     * Used for age verification, demographic reporting, and business rules.
     * Required for all user types for compliance and age-appropriate services.
     */
    @Column(name = "birthdate", nullable = false)
    private LocalDate birthDate;

    /**
     * Date when the user joined/enrolled in the organization.
     * This represents the actual business date, not system creation date.
     * Used for anniversary tracking, tenure calculations, and billing cycles.
     * <p>
     * Note: This should be explicitly set and may differ from created_at
     * (e.g., for historical data migration or future-dated enrollments).
     */
    @Column(name = "entry_date", nullable = false)
    private LocalDate entryDate;

    /**
     * Encrypted profile picture of the user.
     * Stored as binary data for security compliance.
     * Optional field - users may not have profile pictures.
     */
    @Lob
    @Column(name = "encrypted_profile_picture", columnDefinition = "MEDIUMBLOB")
    private byte[] encryptedProfilePicture;
}