/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.currentuser.interfaceadapters;

import com.akademiaplus.currentuser.usecases.GetCurrentUserUseCase;
import openapi.akademiaplus.domain.user.management.api.MeApi;
import openapi.akademiaplus.domain.user.management.dto.GetCurrentUserResponseDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for the {@code /me} endpoint.
 *
 * <p>Resolves the authenticated user's profile from the JWT token
 * and returns the appropriate response based on user type.</p>
 *
 * @author ElatusDev
 * @since 1.0
 */
@RestController
@RequestMapping("/v1/user-management")
public class CurrentUserController implements MeApi {

    private final GetCurrentUserUseCase getCurrentUserUseCase;

    /**
     * Constructs the controller with the required use case.
     *
     * @param getCurrentUserUseCase the use case for resolving the current user
     */
    public CurrentUserController(GetCurrentUserUseCase getCurrentUserUseCase) {
        this.getCurrentUserUseCase = getCurrentUserUseCase;
    }

    @Override
    public ResponseEntity<GetCurrentUserResponseDTO> getCurrentUser() {
        return ResponseEntity.ok(getCurrentUserUseCase.getCurrentUser());
    }
}
