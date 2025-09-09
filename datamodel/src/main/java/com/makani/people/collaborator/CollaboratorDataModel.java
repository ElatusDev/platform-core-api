/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.makani.people.collaborator;

import com.makani.security.user.InternalAuthDataModel;
import com.makani.AbstractPerson;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.io.Serial;
import java.io.Serializable;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Scope("prototype")
@Component
@Entity
@Table(name = "collaborator")
public class CollaboratorDataModel extends AbstractPerson implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "collaborator_id")
    private Integer collaboratorId;

    @Column(nullable = false, length = 100)
    private String skills;

    @Lob
    @Column(name = "encrypted_profile_picture", columnDefinition = "MEDIUMBLOB")
    private Byte[] profilePicture;

    @OneToOne(optional = false, cascade = CascadeType.PERSIST, orphanRemoval = true)
    @JoinColumn(name = "internal_auth_id")
    private InternalAuthDataModel internalAuth;
}
