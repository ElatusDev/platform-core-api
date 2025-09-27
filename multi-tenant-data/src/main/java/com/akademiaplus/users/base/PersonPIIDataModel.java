/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.users.base;

import com.akademiaplus.utilities.security.StringEncryptor;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Scope("prototype")
@Component
@Entity
@Table(name = "person_pii")
public class PersonPIIDataModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "person_pii_id")
    private Integer personPiiId;

    @Convert(converter = StringEncryptor.class)
    @Column(name = "encrypted_first_name", nullable = false)
    private String firstName;

    @Convert(converter = StringEncryptor.class)
    @Column(name = "encrypted_last_name", nullable = false)
    private String lastName;

    @Convert(converter = StringEncryptor.class)
    @Column(name = "encrypted_email", nullable = false)
    private String email;

    @Convert(converter = StringEncryptor.class)
    @Column(name = "encrypted_phone_number", nullable = false)
    private String phone;

    @Convert(converter = StringEncryptor.class)
    @Column(name = "encrypted_address", nullable = false)
    private String address;

    @Convert(converter = StringEncryptor.class)
    @Column(name = "encrypted_zip_code", nullable = false)
    private String zipCode;

    @Column(name = "email_hash", length = 64, nullable = false, unique = true)
    private String emailHash;
    @Column(name = "phone_number_hash", length = 64, nullable = false, unique = true)
    private String phoneHash;
}
