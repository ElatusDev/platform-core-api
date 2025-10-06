/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.internal.usecases;

import com.akademiaplus.infra.config.TenantContextHolder;
import com.akademiaplus.internal.interfaceadapters.InternalAuthRepository;
import com.akademiaplus.security.InternalAuthDataModel;
import com.akademiaplus.utilities.security.HashingService;
import lombok.Setter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
public class InternalAuthorizationUseCase implements UserDetailsService {
    private final InternalAuthRepository repository;
    private final HashingService hashingService;

    @Setter
    private TenantContextHolder tenantContextHolder;

    public InternalAuthorizationUseCase(InternalAuthRepository repository,
                                        HashingService hashingService) {
        this.repository = repository;
        this.hashingService = hashingService;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        String usernameHash = hashingService.generateHash(username);
        Optional<InternalAuthDataModel> result =  repository.findByUsernameHash(usernameHash);

        if(result.isPresent()) {
            InternalAuthDataModel internalAuth = result.get();
            String role = internalAuth.getRole();
            List<GrantedAuthority> list = new ArrayList<>();
            list.add(new SimpleGrantedAuthority("ROLE_" + role));
            return new User(
                    internalAuth.getUsername(),
                    "",
                    true,
                    true,
                    true,
                    true,
                    list
            );
        } else {
            throw new SecurityException("failed to find user for authorization");
        }
    }
}
