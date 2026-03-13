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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@DisplayName("UnregisterPushDeviceUseCase")
@ExtendWith(MockitoExtension.class)
class UnregisterPushDeviceUseCaseTest {

    private static final String DEVICE_TOKEN = "fcm-token-abc123";

    @Mock private PushDeviceRepository pushDeviceRepository;

    private UnregisterPushDeviceUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new UnregisterPushDeviceUseCase(pushDeviceRepository);
    }

    @Nested
    @DisplayName("Unregistration")
    class Unregistration {

        @Test
        @DisplayName("Should delete device when token exists")
        void shouldDeleteDevice_whenTokenExists() {
            // Given
            when(pushDeviceRepository.existsByDeviceToken(DEVICE_TOKEN)).thenReturn(true);

            // When
            useCase.unregister(DEVICE_TOKEN);

            // Then
            verify(pushDeviceRepository, times(1)).existsByDeviceToken(DEVICE_TOKEN);
            verify(pushDeviceRepository, times(1)).deleteByDeviceToken(DEVICE_TOKEN);
            verifyNoMoreInteractions(pushDeviceRepository);
        }

        @Test
        @DisplayName("Should throw EntityNotFoundException when token does not exist")
        void shouldThrowEntityNotFoundException_whenTokenDoesNotExist() {
            // Given
            when(pushDeviceRepository.existsByDeviceToken(DEVICE_TOKEN)).thenReturn(false);

            // When & Then
            assertThatThrownBy(() -> useCase.unregister(DEVICE_TOKEN))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessage(String.format(EntityNotFoundException.MESSAGE_TEMPLATE,
                            EntityType.PUSH_DEVICE, DEVICE_TOKEN));

            // Rule 9 — verify downstream deleteByDeviceToken was NOT called
            verify(pushDeviceRepository, times(1)).existsByDeviceToken(DEVICE_TOKEN);
            verifyNoMoreInteractions(pushDeviceRepository);
        }
    }
}
