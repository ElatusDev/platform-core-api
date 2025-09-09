/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.makani;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.sql.Date;

@Getter
@Setter
@MappedSuperclass
public abstract class AbstractPerson {

    @OneToOne(optional = false, cascade = CascadeType.PERSIST)
    @JoinColumn(name = "person_pii_id")
    private PersonPIIDataModel personPII;

    @Column(name ="birthdate", nullable = false)
    private Date birthDate;
}
