/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.notification.interfaceadapters;

import com.akademiaplus.notification.usecases.EmailDeliveryManagementUseCase;
import com.akademiaplus.notification.usecases.EmailTemplateCreationUseCase;
import com.akademiaplus.notification.usecases.EmailTemplatePreviewUseCase;
import com.akademiaplus.notification.usecases.EmailTemplateUpdateUseCase;
import com.akademiaplus.notification.usecases.GetEmailTemplateByIdUseCase;
import com.akademiaplus.notification.usecases.GetNotificationByIdUseCase;
import com.akademiaplus.notification.usecases.ImmediateSendUseCase;
import com.akademiaplus.notification.usecases.ListEmailTemplatesUseCase;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import openapi.akademiaplus.domain.notification.system.dto.CreateEmailTemplateRequestDTO;
import openapi.akademiaplus.domain.notification.system.dto.EmailDeliveryDetailResponseDTO;
import openapi.akademiaplus.domain.notification.system.dto.EmailTemplateListResponseDTO;
import openapi.akademiaplus.domain.notification.system.dto.EmailTemplateResponseDTO;
import openapi.akademiaplus.domain.notification.system.dto.ImmediateEmailDeliveryResponseDTO;
import openapi.akademiaplus.domain.notification.system.dto.ImmediateEmailNotificationRequestDTO;
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

import java.util.Collections;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("EmailController")
@ExtendWith(MockitoExtension.class)
class EmailControllerTest {

    private static final String BASE_PATH = "/v1/notification-system/email";
    private static final String TENANT_HEADER = "X-Tenant-ID";
    private static final String TENANT_ID_VALUE = "1";
    private static final Long DELIVERY_ID = 1L;
    private static final Long NOTIFICATION_ID = 10L;
    private static final String TEMPLATE_NAME = "Welcome Email";
    private static final String TEMPLATE_CATEGORY = "onboarding";
    private static final String TEMPLATE_SUBJECT = "Welcome to Akademia!";
    private static final String TEMPLATE_BODY_HTML = "<h1>Welcome</h1>";
    private static final String IMMEDIATE_SUBJECT = "Urgent Notification";
    private static final String IMMEDIATE_BODY = "<p>Important message</p>";

    @Mock private EmailDeliveryManagementUseCase emailDeliveryManagementUseCase;
    @Mock private ImmediateSendUseCase immediateSendUseCase;
    @Mock private EmailTemplateCreationUseCase emailTemplateCreationUseCase;
    @Mock private EmailTemplateUpdateUseCase emailTemplateUpdateUseCase;
    @Mock private GetEmailTemplateByIdUseCase getEmailTemplateByIdUseCase;
    @Mock private ListEmailTemplatesUseCase listEmailTemplatesUseCase;
    @Mock private EmailTemplatePreviewUseCase emailTemplatePreviewUseCase;
    @Mock private GetNotificationByIdUseCase getNotificationByIdUseCase;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        EmailController controller = new EmailController(
                emailDeliveryManagementUseCase,
                immediateSendUseCase,
                emailTemplateCreationUseCase,
                emailTemplateUpdateUseCase,
                getEmailTemplateByIdUseCase,
                listEmailTemplatesUseCase,
                emailTemplatePreviewUseCase,
                getNotificationByIdUseCase);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    @Nested
    @DisplayName("POST /email/send — Immediate Send")
    class ImmediateSend {

        @Test
        @DisplayName("Should return 200 when immediate send succeeds")
        void shouldReturn200_whenImmediateSendSucceeds() throws Exception {
            // Given
            ImmediateEmailNotificationRequestDTO requestDTO = new ImmediateEmailNotificationRequestDTO();
            requestDTO.setSubject(IMMEDIATE_SUBJECT);
            requestDTO.setBody(IMMEDIATE_BODY);

            ImmediateEmailDeliveryResponseDTO responseDTO = new ImmediateEmailDeliveryResponseDTO();
            responseDTO.setMessage(ImmediateSendUseCase.MESSAGE_SENT);
            responseDTO.setDeliveryResults(Collections.emptyList());

            when(immediateSendUseCase.send(requestDTO)).thenReturn(responseDTO);

            // When & Then
            mockMvc.perform(post(BASE_PATH + "/send")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header(TENANT_HEADER, TENANT_ID_VALUE)
                            .content(objectMapper.writeValueAsString(requestDTO)))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.message").value(ImmediateSendUseCase.MESSAGE_SENT));

            verify(immediateSendUseCase).send(requestDTO);
            verifyNoMoreInteractions(immediateSendUseCase);
        }
    }

    @Nested
    @DisplayName("Template Operations")
    class TemplateOperations {

        @Test
        @DisplayName("Should return 201 when template created")
        void shouldReturn201_whenTemplateCreated() throws Exception {
            // Given
            CreateEmailTemplateRequestDTO requestDTO = new CreateEmailTemplateRequestDTO();
            requestDTO.setName(TEMPLATE_NAME);
            requestDTO.setCategory(TEMPLATE_CATEGORY);
            requestDTO.setSubject(TEMPLATE_SUBJECT);
            requestDTO.setBodyHtml(TEMPLATE_BODY_HTML);

            EmailTemplateResponseDTO responseDTO = new EmailTemplateResponseDTO();
            responseDTO.setName(TEMPLATE_NAME);
            responseDTO.setCategory(TEMPLATE_CATEGORY);

            when(emailTemplateCreationUseCase.create(requestDTO)).thenReturn(responseDTO);

            // When & Then
            mockMvc.perform(post(BASE_PATH + "/templates")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header(TENANT_HEADER, TENANT_ID_VALUE)
                            .content(objectMapper.writeValueAsString(requestDTO)))
                    .andExpect(status().isCreated())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.name").value(TEMPLATE_NAME))
                    .andExpect(jsonPath("$.category").value(TEMPLATE_CATEGORY));

            verify(emailTemplateCreationUseCase).create(requestDTO);
            verifyNoMoreInteractions(emailTemplateCreationUseCase);
        }

        @Test
        @DisplayName("Should return 200 when templates listed")
        void shouldReturn200_whenTemplatesListed() throws Exception {
            // Given
            EmailTemplateListResponseDTO responseDTO = new EmailTemplateListResponseDTO();
            responseDTO.setTemplates(Collections.emptyList());

            when(listEmailTemplatesUseCase.list(null)).thenReturn(responseDTO);

            // When & Then
            mockMvc.perform(get(BASE_PATH + "/templates")
                            .accept(MediaType.APPLICATION_JSON)
                            .header(TENANT_HEADER, TENANT_ID_VALUE))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.templates").isArray());

            verify(listEmailTemplatesUseCase).list(null);
            verifyNoMoreInteractions(listEmailTemplatesUseCase);
        }
    }

    @Nested
    @DisplayName("Delivery Operations")
    class DeliveryOperations {

        @Test
        @DisplayName("Should return 200 when delivery retrieved")
        void shouldReturn200_whenDeliveryRetrieved() throws Exception {
            // Given
            EmailDeliveryDetailResponseDTO responseDTO = new EmailDeliveryDetailResponseDTO();
            responseDTO.setNotificationId(NOTIFICATION_ID);

            when(emailDeliveryManagementUseCase.getDeliveryById(DELIVERY_ID)).thenReturn(responseDTO);

            // When & Then
            mockMvc.perform(get(BASE_PATH + "/deliveries/{deliveryId}", DELIVERY_ID)
                            .accept(MediaType.APPLICATION_JSON)
                            .header(TENANT_HEADER, TENANT_ID_VALUE))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.notificationId").value(NOTIFICATION_ID));

            verify(emailDeliveryManagementUseCase).getDeliveryById(DELIVERY_ID);
            verifyNoMoreInteractions(emailDeliveryManagementUseCase);
        }
    }
}
