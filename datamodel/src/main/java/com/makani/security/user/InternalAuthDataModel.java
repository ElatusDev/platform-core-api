package com.makani.security.user;

import com.makani.utilities.security.StringEncryptor;
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
@Table(name = "internal_auth")
public class InternalAuthDataModel implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "internal_auth_id")
    private Integer internalAuthId;

    @Convert(converter = StringEncryptor.class)
    @Column(name = "encrypted_username",nullable = false)
    private String username;

    @Convert(converter = StringEncryptor.class)
    @Column(name = "encrypted_password",nullable = false)
    private String password;

    @Convert(converter = StringEncryptor.class)
    @Column(name ="encrypted_role", nullable = false)
    private String role;

    @Column(name = "username_hash", length = 64, nullable = false, unique = true)
    private String usernameHash;
}
