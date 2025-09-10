package com.makani.internal.usecases;

import com.makani.exceptions.InvalidLoginException;
import com.makani.security.user.InternalAuthDataModel;
import com.makani.internal.interfaceadapters.InternalAuthRepository;
import com.makani.internal.interfaceadapters.jwt.JwtTokenProvider;
import com.makani.utilities.security.HashingService;
import openapi.makani.domain.security.dto.AuthTokenResponseDTO;
import openapi.makani.domain.security.dto.LoginRequestDTO;
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
        return new AuthTokenResponseDTO(jwtTokenProvider.createToken(auth.getUsername(), claims));
    }

}
