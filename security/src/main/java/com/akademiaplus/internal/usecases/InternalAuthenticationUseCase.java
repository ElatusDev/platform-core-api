/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.internal.usecases;

import com.akademiaplus.exceptions.InvalidLoginException;
import com.akademiaplus.security.InternalAuthDataModel;
import com.akademiaplus.internal.interfaceadapters.InternalAuthRepository;
import com.akademiaplus.internal.interfaceadapters.jwt.JwtTokenProvider;
import com.akademiaplus.utilities.security.HashingService;
import openapi.akademiaplus.domain.security.dto.AuthTokenResponseDTO;
import openapi.akademiaplus.domain.security.dto.LoginRequestDTO;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class InternalAuthenticationUseCase {
    private final InternalAuthRepository repository;
    private final JwtTokenProvider jwtTokenProvider;
    private final HashingService hashingService;

    public InternalAuthenticationUseCase(InternalAuthRepository repository,
                                         JwtTokenProvider jwtTokenProvider,
                                         HashingService hashingService) {
        this.repository = repository;
        this.jwtTokenProvider = jwtTokenProvider;
        this.hashingService = hashingService;
    }

    public AuthTokenResponseDTO login(LoginRequestDTO dto) {
        String usernameHash = hashingService.generateHash(dto.getUsername());
        InternalAuthDataModel auth = repository.findByUsernameHash(usernameHash)
                .filter(user -> dto.getPassword().equals(user.getPassword()))
                .orElseThrow(InvalidLoginException::new);

        Map<String,Object> claims = new HashMap<>();
        claims.put("Has role", auth.getRole());
        return new AuthTokenResponseDTO(jwtTokenProvider.createToken(auth.getUsername(), auth.getTenantId() ,claims));
    }

}
