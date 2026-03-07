/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.notification.usecases;

import com.akademiaplus.notification.interfaceadapters.PushDeviceRepository;
import com.akademiaplus.notifications.PushDeviceDataModel;
import openapi.akademiaplus.domain.notification.system.dto.RegisterPushDeviceRequestDTO;
import openapi.akademiaplus.domain.notification.system.dto.RegisterPushDeviceResponseDTO;
import org.springframework.stereotype.Service;

/**
 * Use case for registering or updating a push device token.
 * <p>
 * Performs an upsert: if a device with the same token already exists,
 * updates the userId and platform; otherwise inserts a new record.
 *
 * @author ElatusDev
 * @since 1.0
 */
@Service
public class RegisterPushDeviceUseCase {

    private final PushDeviceRepository pushDeviceRepository;

    public RegisterPushDeviceUseCase(PushDeviceRepository pushDeviceRepository) {
        this.pushDeviceRepository = pushDeviceRepository;
    }

    public RegisterPushDeviceResponseDTO register(RegisterPushDeviceRequestDTO request) {
        PushDeviceDataModel device = pushDeviceRepository.findByDeviceToken(request.getDeviceToken())
                .orElseGet(PushDeviceDataModel::new);

        device.setUserId(request.getUserId());
        device.setDeviceToken(request.getDeviceToken());
        device.setPlatform(request.getPlatform().getValue());
        device.setAppVersion(request.getAppVersion());

        PushDeviceDataModel saved = pushDeviceRepository.save(device);

        RegisterPushDeviceResponseDTO response = new RegisterPushDeviceResponseDTO();
        response.setPushDeviceId(saved.getPushDeviceId());
        response.setDeviceToken(saved.getDeviceToken());
        response.setPlatform(saved.getPlatform());
        return response;
    }
}
