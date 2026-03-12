/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.notification.interfaceadapters;

import com.akademiaplus.config.NotificationControllerAdvice;
import com.akademiaplus.notification.usecases.RegisterPushDeviceUseCase;
import com.akademiaplus.notification.usecases.UnregisterPushDeviceUseCase;
import com.akademiaplus.utilities.EntityType;
import com.akademiaplus.utilities.MessageService;
import com.akademiaplus.utilities.exceptions.EntityNotFoundException;
import openapi.akademiaplus.domain.notification.system.dto.RegisterPushDeviceRequestDTO;
import openapi.akademiaplus.domain.notification.system.dto.RegisterPushDeviceResponseDTO;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("PushDeviceController")
@ExtendWith(MockitoExtension.class)
class PushDeviceControllerTest {

    private static final String BASE_PATH = "/v1/notification-system/push/devices";
    private static final String DEVICE_TOKEN = "fcm-token-abc123";

    @Mock private RegisterPushDeviceUseCase registerPushDeviceUseCase;
    @Mock private UnregisterPushDeviceUseCase unregisterPushDeviceUseCase;
    @Mock private MessageService messageService;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        PushDeviceController controller = new PushDeviceController(
                registerPushDeviceUseCase, unregisterPushDeviceUseCase);
        NotificationControllerAdvice controllerAdvice = new NotificationControllerAdvice(messageService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(controllerAdvice)
                .build();
        objectMapper = new ObjectMapper();
    }

    @Nested
    @DisplayName("POST /push/devices")
    class RegisterDevice {

        @Test
        @DisplayName("Should return 201 when device registered successfully")
        void shouldReturn201_whenDeviceRegisteredSuccessfully() throws Exception {
            // Given
            RegisterPushDeviceRequestDTO request = new RegisterPushDeviceRequestDTO(
                    42L, DEVICE_TOKEN, RegisterPushDeviceRequestDTO.PlatformEnum.ANDROID);

            RegisterPushDeviceResponseDTO response = new RegisterPushDeviceResponseDTO();
            response.setPushDeviceId(1L);
            response.setDeviceToken(DEVICE_TOKEN);
            response.setPlatform("ANDROID");

            when(registerPushDeviceUseCase.register(request)).thenReturn(response);

            // When & Then
            mockMvc.perform(post(BASE_PATH)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.pushDeviceId").value(1L))
                    .andExpect(jsonPath("$.deviceToken").value(DEVICE_TOKEN))
                    .andExpect(jsonPath("$.platform").value("ANDROID"));

            verify(registerPushDeviceUseCase, times(1)).register(request);
            verifyNoMoreInteractions(registerPushDeviceUseCase, unregisterPushDeviceUseCase, messageService);
        }
    }

    @Nested
    @DisplayName("DELETE /push/devices/{deviceToken}")
    class UnregisterDevice {

        @Test
        @DisplayName("Should return 204 when device unregistered successfully")
        void shouldReturn204_whenDeviceUnregisteredSuccessfully() throws Exception {
            // When & Then
            mockMvc.perform(delete(BASE_PATH + "/{deviceToken}", DEVICE_TOKEN))
                    .andExpect(status().isNoContent());

            verify(unregisterPushDeviceUseCase, times(1)).unregister(DEVICE_TOKEN);
            verifyNoMoreInteractions(registerPushDeviceUseCase, unregisterPushDeviceUseCase, messageService);
        }

        @Test
        @DisplayName("Should return 404 when device token not found")
        void shouldReturn404_whenDeviceTokenNotFound() throws Exception {
            // Given
            doThrow(new EntityNotFoundException(EntityType.PUSH_DEVICE, DEVICE_TOKEN))
                    .when(unregisterPushDeviceUseCase).unregister(DEVICE_TOKEN);
            when(messageService.getEntityNotFound(EntityType.PUSH_DEVICE, DEVICE_TOKEN))
                    .thenReturn("Push device not found: " + DEVICE_TOKEN);

            // When & Then
            mockMvc.perform(delete(BASE_PATH + "/{deviceToken}", DEVICE_TOKEN))
                    .andExpect(status().isNotFound());

            verify(unregisterPushDeviceUseCase, times(1)).unregister(DEVICE_TOKEN);
            verify(messageService, times(1)).getEntityNotFound(EntityType.PUSH_DEVICE, DEVICE_TOKEN);
            verifyNoMoreInteractions(registerPushDeviceUseCase, unregisterPushDeviceUseCase, messageService);
        }
    }
}
