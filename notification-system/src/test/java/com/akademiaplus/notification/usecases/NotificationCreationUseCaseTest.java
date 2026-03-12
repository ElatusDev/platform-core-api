/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.notification.usecases;

import com.akademiaplus.notification.interfaceadapters.NotificationRepository;
import com.akademiaplus.notifications.NotificationDataModel;
import com.akademiaplus.notifications.NotificationPriority;
import com.akademiaplus.notifications.NotificationType;
import openapi.akademiaplus.domain.notification.system.dto.NotificationCreationRequestDTO;
import openapi.akademiaplus.domain.notification.system.dto.NotificationCreationResponseDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.springframework.context.ApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@DisplayName("NotificationCreationUseCase")
@ExtendWith(MockitoExtension.class)
class NotificationCreationUseCaseTest {

    @Mock private ApplicationContext applicationContext;
    @Mock private NotificationRepository notificationRepository;
    @Mock private ModelMapper modelMapper;

    private NotificationCreationUseCase useCase;

    private static final String TITLE = "Course Reminder";
    private static final String CONTENT = "Your class starts in 30 minutes";
    private static final String TYPE = "COURSE_REMINDER";
    private static final String PRIORITY = "HIGH";
    private static final Long SAVED_ID = 1L;

    @BeforeEach
    void setUp() {
        useCase = new NotificationCreationUseCase(
                applicationContext, notificationRepository, modelMapper);
    }

    private NotificationCreationRequestDTO buildDto() {
        NotificationCreationRequestDTO dto = new NotificationCreationRequestDTO();
        dto.setTitle(TITLE);
        dto.setContent(CONTENT);
        dto.setType(TYPE);
        dto.setPriority(PRIORITY);
        return dto;
    }

    @Nested
    @DisplayName("Transformation")
    class Transformation {

        @Test
        @DisplayName("Should retrieve prototype bean from ApplicationContext")
        void shouldRetrievePrototypeBean_whenTransforming() {
            // Given
            NotificationCreationRequestDTO dto = buildDto();
            NotificationDataModel prototypeModel = new NotificationDataModel();
            when(applicationContext.getBean(NotificationDataModel.class)).thenReturn(prototypeModel);

            // When
            useCase.transform(dto);

            // Then
            verify(applicationContext, times(1)).getBean(NotificationDataModel.class);
            verifyNoMoreInteractions(applicationContext, notificationRepository, modelMapper);
        }

        @Test
        @DisplayName("Should delegate mapping to ModelMapper with named TypeMap")
        void shouldDelegateToModelMapper_whenTransforming() {
            // Given
            NotificationCreationRequestDTO dto = buildDto();
            NotificationDataModel prototypeModel = new NotificationDataModel();
            when(applicationContext.getBean(NotificationDataModel.class)).thenReturn(prototypeModel);

            // When
            NotificationDataModel result = useCase.transform(dto);

            // Then
            verify(applicationContext, times(1)).getBean(NotificationDataModel.class);
            verify(modelMapper, times(1)).map(dto, prototypeModel, NotificationCreationUseCase.MAP_NAME);
            assertThat(result).isSameAs(prototypeModel);
            verifyNoMoreInteractions(applicationContext, notificationRepository, modelMapper);
        }

        @Test
        @DisplayName("Should convert type string to NotificationType enum")
        void shouldConvertTypeString_whenTypeIsProvided() {
            // Given
            NotificationCreationRequestDTO dto = buildDto();
            NotificationDataModel prototypeModel = new NotificationDataModel();
            when(applicationContext.getBean(NotificationDataModel.class)).thenReturn(prototypeModel);

            // When
            NotificationDataModel result = useCase.transform(dto);

            // Then
            assertThat(result.getType()).isEqualTo(NotificationType.COURSE_REMINDER);
            verify(applicationContext, times(1)).getBean(NotificationDataModel.class);
            verifyNoMoreInteractions(applicationContext, notificationRepository, modelMapper);
        }

        @Test
        @DisplayName("Should convert priority string to NotificationPriority enum")
        void shouldConvertPriorityString_whenPriorityIsProvided() {
            // Given
            NotificationCreationRequestDTO dto = buildDto();
            NotificationDataModel prototypeModel = new NotificationDataModel();
            when(applicationContext.getBean(NotificationDataModel.class)).thenReturn(prototypeModel);

            // When
            NotificationDataModel result = useCase.transform(dto);

            // Then
            assertThat(result.getPriority()).isEqualTo(NotificationPriority.HIGH);
            verify(applicationContext, times(1)).getBean(NotificationDataModel.class);
            verifyNoMoreInteractions(applicationContext, notificationRepository, modelMapper);
        }

        @Test
        @DisplayName("Should leave type null when DTO type is null")
        void shouldLeaveTypeNull_whenDtoTypeIsNull() {
            // Given
            NotificationCreationRequestDTO dto = buildDto();
            dto.setType(null);
            NotificationDataModel prototypeModel = new NotificationDataModel();
            when(applicationContext.getBean(NotificationDataModel.class)).thenReturn(prototypeModel);

            // When
            NotificationDataModel result = useCase.transform(dto);

            // Then
            assertThat(result.getType()).isNull();
            verify(applicationContext, times(1)).getBean(NotificationDataModel.class);
            verifyNoMoreInteractions(applicationContext, notificationRepository, modelMapper);
        }

        @Test
        @DisplayName("Should leave priority null when DTO priority is null")
        void shouldLeavePriorityNull_whenDtoPriorityIsNull() {
            // Given
            NotificationCreationRequestDTO dto = buildDto();
            dto.setPriority(null);
            NotificationDataModel prototypeModel = new NotificationDataModel();
            when(applicationContext.getBean(NotificationDataModel.class)).thenReturn(prototypeModel);

            // When
            NotificationDataModel result = useCase.transform(dto);

            // Then
            assertThat(result.getPriority()).isNull();
            verify(applicationContext, times(1)).getBean(NotificationDataModel.class);
            verifyNoMoreInteractions(applicationContext, notificationRepository, modelMapper);
        }
    }

    @Nested
    @DisplayName("Persistence")
    class Persistence {

        @Test
        @DisplayName("Should save transformed model and return mapped DTO")
        void shouldSaveAndReturnDto_whenCreating() {
            // Given
            NotificationCreationRequestDTO dto = buildDto();
            NotificationDataModel prototypeModel = new NotificationDataModel();
            NotificationDataModel savedModel = new NotificationDataModel();
            savedModel.setNotificationId(SAVED_ID);
            NotificationCreationResponseDTO expectedDto = new NotificationCreationResponseDTO();
            expectedDto.setNotificationId(SAVED_ID);

            when(applicationContext.getBean(NotificationDataModel.class)).thenReturn(prototypeModel);
            doNothing().when(modelMapper).map(dto, prototypeModel, NotificationCreationUseCase.MAP_NAME);
            when(notificationRepository.saveAndFlush(prototypeModel)).thenReturn(savedModel);
            when(modelMapper.map(savedModel, NotificationCreationResponseDTO.class)).thenReturn(expectedDto);

            // When
            NotificationCreationResponseDTO result = useCase.create(dto);

            // Then
            verify(notificationRepository, times(1)).saveAndFlush(prototypeModel);
            assertThat(result.getNotificationId()).isEqualTo(SAVED_ID);
            verifyNoMoreInteractions(applicationContext, notificationRepository, modelMapper);
        }

        @Test
        @DisplayName("Should execute operations in correct order")
        void shouldExecuteInOrder_whenCreating() {
            // Given
            NotificationCreationRequestDTO dto = buildDto();
            NotificationDataModel prototypeModel = new NotificationDataModel();
            NotificationDataModel savedModel = new NotificationDataModel();
            NotificationCreationResponseDTO responseDto = new NotificationCreationResponseDTO();

            when(applicationContext.getBean(NotificationDataModel.class)).thenReturn(prototypeModel);
            doNothing().when(modelMapper).map(dto, prototypeModel, NotificationCreationUseCase.MAP_NAME);
            when(notificationRepository.saveAndFlush(prototypeModel)).thenReturn(savedModel);
            when(modelMapper.map(savedModel, NotificationCreationResponseDTO.class)).thenReturn(responseDto);

            // When
            useCase.create(dto);

            // Then
            InOrder inOrder = inOrder(applicationContext, modelMapper, notificationRepository);
            inOrder.verify(applicationContext, times(1)).getBean(NotificationDataModel.class);
            inOrder.verify(modelMapper, times(1)).map(dto, prototypeModel, NotificationCreationUseCase.MAP_NAME);
            inOrder.verify(notificationRepository, times(1)).saveAndFlush(prototypeModel);
            inOrder.verify(modelMapper, times(1)).map(savedModel, NotificationCreationResponseDTO.class);
            inOrder.verifyNoMoreInteractions();
        }
    }
}
