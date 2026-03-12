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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import org.mockito.InOrder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@DisplayName("RegisterPushDeviceUseCase")
@ExtendWith(MockitoExtension.class)
class RegisterPushDeviceUseCaseTest {

    private static final Long USER_ID = 42L;
    private static final String DEVICE_TOKEN = "fcm-token-abc123";
    private static final String APP_VERSION = "2.1.0";

    @Mock private PushDeviceRepository pushDeviceRepository;

    private RegisterPushDeviceUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new RegisterPushDeviceUseCase(pushDeviceRepository);
    }

    @Nested
    @DisplayName("Registration")
    class Registration {

        @Test
        @DisplayName("Should create new device when token does not exist")
        void shouldCreateNewDevice_whenTokenDoesNotExist() {
            // Given
            RegisterPushDeviceRequestDTO request = new RegisterPushDeviceRequestDTO(
                    USER_ID, DEVICE_TOKEN, RegisterPushDeviceRequestDTO.PlatformEnum.ANDROID);
            request.setAppVersion(APP_VERSION);

            PushDeviceDataModel savedDevice = new PushDeviceDataModel();
            savedDevice.setPushDeviceId(1L);
            savedDevice.setDeviceToken(DEVICE_TOKEN);
            savedDevice.setPlatform("ANDROID");

            when(pushDeviceRepository.findByDeviceToken(DEVICE_TOKEN)).thenReturn(Optional.empty());

            ArgumentCaptor<PushDeviceDataModel> captor = ArgumentCaptor.forClass(PushDeviceDataModel.class);
            when(pushDeviceRepository.save(captor.capture())).thenReturn(savedDevice);

            // When
            RegisterPushDeviceResponseDTO result = useCase.register(request);

            // Then
            assertThat(result.getPushDeviceId()).isEqualTo(1L);
            assertThat(result.getDeviceToken()).isEqualTo(DEVICE_TOKEN);
            assertThat(result.getPlatform()).isEqualTo("ANDROID");

            PushDeviceDataModel captured = captor.getValue();
            assertThat(captured.getUserId()).isEqualTo(USER_ID);
            assertThat(captured.getDeviceToken()).isEqualTo(DEVICE_TOKEN);
            assertThat(captured.getPlatform()).isEqualTo("ANDROID");
            assertThat(captured.getAppVersion()).isEqualTo(APP_VERSION);
            InOrder inOrder = inOrder(pushDeviceRepository);
            inOrder.verify(pushDeviceRepository, times(1)).findByDeviceToken(DEVICE_TOKEN);
            inOrder.verify(pushDeviceRepository, times(1)).save(captured);
            inOrder.verifyNoMoreInteractions();
        }

        @Test
        @DisplayName("Should update existing device when token already exists")
        void shouldUpdateExistingDevice_whenTokenAlreadyExists() {
            // Given
            Long existingDeviceId = 10L;
            PushDeviceDataModel existingDevice = new PushDeviceDataModel();
            existingDevice.setPushDeviceId(existingDeviceId);
            existingDevice.setUserId(99L);
            existingDevice.setDeviceToken(DEVICE_TOKEN);
            existingDevice.setPlatform("IOS");

            RegisterPushDeviceRequestDTO request = new RegisterPushDeviceRequestDTO(
                    USER_ID, DEVICE_TOKEN, RegisterPushDeviceRequestDTO.PlatformEnum.ANDROID);

            PushDeviceDataModel savedDevice = new PushDeviceDataModel();
            savedDevice.setPushDeviceId(existingDeviceId);
            savedDevice.setDeviceToken(DEVICE_TOKEN);
            savedDevice.setPlatform("ANDROID");

            when(pushDeviceRepository.findByDeviceToken(DEVICE_TOKEN)).thenReturn(Optional.of(existingDevice));
            when(pushDeviceRepository.save(existingDevice)).thenReturn(savedDevice);

            // When
            RegisterPushDeviceResponseDTO result = useCase.register(request);

            // Then
            assertThat(result.getPushDeviceId()).isEqualTo(existingDeviceId);
            verify(pushDeviceRepository).save(existingDevice);
            assertThat(existingDevice.getUserId()).isEqualTo(USER_ID);
            assertThat(existingDevice.getPlatform()).isEqualTo("ANDROID");
        }
    }
}
