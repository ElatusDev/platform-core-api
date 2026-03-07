/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.notification.interfaceadapters;

import com.akademiaplus.notification.usecases.RegisterPushDeviceUseCase;
import com.akademiaplus.notification.usecases.UnregisterPushDeviceUseCase;
import openapi.akademiaplus.domain.notification.system.api.PushApi;
import openapi.akademiaplus.domain.notification.system.dto.RegisterPushDeviceRequestDTO;
import openapi.akademiaplus.domain.notification.system.dto.RegisterPushDeviceResponseDTO;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for push device token management.
 *
 * @author ElatusDev
 * @since 1.0
 */
@RestController
@RequestMapping("/v1/notification-system")
public class PushDeviceController implements PushApi {

    private final RegisterPushDeviceUseCase registerPushDeviceUseCase;
    private final UnregisterPushDeviceUseCase unregisterPushDeviceUseCase;

    public PushDeviceController(RegisterPushDeviceUseCase registerPushDeviceUseCase,
                                UnregisterPushDeviceUseCase unregisterPushDeviceUseCase) {
        this.registerPushDeviceUseCase = registerPushDeviceUseCase;
        this.unregisterPushDeviceUseCase = unregisterPushDeviceUseCase;
    }

    @Override
    public ResponseEntity<RegisterPushDeviceResponseDTO> registerPushDevice(
            RegisterPushDeviceRequestDTO registerPushDeviceRequestDTO) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(registerPushDeviceUseCase.register(registerPushDeviceRequestDTO));
    }

    @Override
    public ResponseEntity<Void> unregisterPushDevice(String deviceToken) {
        unregisterPushDeviceUseCase.unregister(deviceToken);
        return ResponseEntity.noContent().build();
    }
}
