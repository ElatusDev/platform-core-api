/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.notification.usecases;

import com.akademiaplus.notification.interfaceadapters.PushDeviceRepository;
import com.akademiaplus.utilities.EntityType;
import com.akademiaplus.utilities.exceptions.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Use case for unregistering a push device token.
 *
 * @author ElatusDev
 * @since 1.0
 */
@Service
public class UnregisterPushDeviceUseCase {

    private final PushDeviceRepository pushDeviceRepository;

    public UnregisterPushDeviceUseCase(PushDeviceRepository pushDeviceRepository) {
        this.pushDeviceRepository = pushDeviceRepository;
    }

    @Transactional
    public void unregister(String deviceToken) {
        if (!pushDeviceRepository.existsByDeviceToken(deviceToken)) {
            throw new EntityNotFoundException(EntityType.PUSH_DEVICE, deviceToken);
        }
        pushDeviceRepository.deleteByDeviceToken(deviceToken);
    }
}
