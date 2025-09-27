/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.users.base;

import com.akademiaplus.TenantAndSoftDeleteAwareEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

/**
 * Abstract base class for all user types in the platform.
 * Provides common user attributes and relationships with personal information.
 * All platform users (employees, students, tutors, collaborators) extend this class.
 */
@Getter
@Setter
@MappedSuperclass
public abstract class AbstractUser extends TenantAndSoftDeleteAwareEntity {

    /**
     * Reference to the user's personal and private information.
     * Uses composite foreign key to maintain tenant isolation.
     */
    @OneToOne(optional = false, cascade = CascadeType.PERSIST, orphanRemoval = true)
    @JoinColumn(name = "tenant_id", referencedColumnName = "tenant_id")
    @JoinColumn(name = "person_pii_id", referencedColumnName = "person_pii_id")
    private PersonPIIDataModel personPII;

    @Column(name = "birthdate", nullable = false)
    private LocalDate birthDate;

    @Column(name = "entry_date", nullable = false)
    private LocalDate entryDate;

    @Lob
    @Column(name = "encrypted_profile_picture")
    private byte[] encryptedProfilePicture;

    @PrePersist
    protected void onUserPrePersist() {
        if (this.entryDate == null) {
            this.entryDate = LocalDate.now();
        }
    }
}