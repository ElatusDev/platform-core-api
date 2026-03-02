/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.membership.usecases;

import com.akademiaplus.billing.membership.MembershipDataModel;
import com.akademiaplus.membership.interfaceadapters.MembershipRepository;
import openapi.akademiaplus.domain.billing.dto.MembershipCreationRequestDTO;
import openapi.akademiaplus.domain.billing.dto.MembershipCreationResponseDTO;
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
import static org.mockito.Mockito.*;

@DisplayName("MembershipCreationUseCase")
@ExtendWith(MockitoExtension.class)
class MembershipCreationUseCaseTest {

    @Mock private ApplicationContext applicationContext;
    @Mock private MembershipRepository membershipRepository;
    @Mock private ModelMapper modelMapper;

    private MembershipCreationUseCase useCase;

    private static final String MEMBERSHIP_TYPE = "PREMIUM";
    private static final Double FEE = 500.0;
    private static final String DESCRIPTION = "Premium membership plan";
    private static final Long SAVED_ID = 1L;

    @BeforeEach
    void setUp() {
        useCase = new MembershipCreationUseCase(applicationContext, membershipRepository, modelMapper);
    }

    private MembershipCreationRequestDTO buildDto() {
        MembershipCreationRequestDTO dto = new MembershipCreationRequestDTO();
        dto.setMembershipType(MEMBERSHIP_TYPE);
        dto.setFee(FEE);
        dto.setDescription(DESCRIPTION);
        return dto;
    }

    @Nested
    @DisplayName("Transformation")
    class Transformation {

        @Test
        @DisplayName("Should retrieve prototype MembershipDataModel from ApplicationContext")
        void shouldRetrievePrototypeBean_whenTransforming() {
            // Given
            MembershipCreationRequestDTO dto = buildDto();
            MembershipDataModel prototypeModel = new MembershipDataModel();
            when(applicationContext.getBean(MembershipDataModel.class)).thenReturn(prototypeModel);

            // When
            useCase.transform(dto);

            // Then
            verify(applicationContext).getBean(MembershipDataModel.class);
        }

        @Test
        @DisplayName("Should delegate mapping to ModelMapper with named TypeMap")
        void shouldDelegateToModelMapper_whenTransforming() {
            // Given
            MembershipCreationRequestDTO dto = buildDto();
            MembershipDataModel prototypeModel = new MembershipDataModel();
            when(applicationContext.getBean(MembershipDataModel.class)).thenReturn(prototypeModel);

            // When
            MembershipDataModel result = useCase.transform(dto);

            // Then
            verify(modelMapper).map(dto, prototypeModel, MembershipCreationUseCase.MAP_NAME);
            assertThat(result).isSameAs(prototypeModel);
        }
    }

    @Nested
    @DisplayName("Persistence")
    class Persistence {

        @Test
        @DisplayName("Should save transformed model and return mapped DTO")
        void shouldSaveAndReturnDto_whenCreating() {
            // Given
            MembershipCreationRequestDTO dto = buildDto();
            MembershipDataModel prototypeModel = new MembershipDataModel();
            MembershipDataModel savedModel = new MembershipDataModel();
            savedModel.setMembershipId(SAVED_ID);
            MembershipCreationResponseDTO expectedDto = new MembershipCreationResponseDTO();
            expectedDto.setMembershipId(SAVED_ID);

            when(applicationContext.getBean(MembershipDataModel.class)).thenReturn(prototypeModel);
            doNothing().when(modelMapper).map(dto, prototypeModel, MembershipCreationUseCase.MAP_NAME);
            when(membershipRepository.saveAndFlush(prototypeModel)).thenReturn(savedModel);
            when(modelMapper.map(savedModel, MembershipCreationResponseDTO.class)).thenReturn(expectedDto);

            // When
            MembershipCreationResponseDTO result = useCase.create(dto);

            // Then
            verify(membershipRepository).saveAndFlush(prototypeModel);
            verify(modelMapper).map(savedModel, MembershipCreationResponseDTO.class);
            assertThat(result.getMembershipId()).isEqualTo(SAVED_ID);
        }

        @Test
        @DisplayName("Should pass transform result directly to repository save")
        void shouldPassTransformResultToSave_whenCreating() {
            // Given
            MembershipCreationRequestDTO dto = buildDto();
            MembershipDataModel prototypeModel = new MembershipDataModel();
            MembershipDataModel savedModel = new MembershipDataModel();
            MembershipCreationResponseDTO responseDto = new MembershipCreationResponseDTO();

            when(applicationContext.getBean(MembershipDataModel.class)).thenReturn(prototypeModel);
            doNothing().when(modelMapper).map(dto, prototypeModel, MembershipCreationUseCase.MAP_NAME);
            when(membershipRepository.saveAndFlush(prototypeModel)).thenReturn(savedModel);
            when(modelMapper.map(savedModel, MembershipCreationResponseDTO.class)).thenReturn(responseDto);

            // When
            useCase.create(dto);

            // Then
            InOrder inOrder = inOrder(applicationContext, modelMapper, membershipRepository);
            inOrder.verify(applicationContext).getBean(MembershipDataModel.class);
            inOrder.verify(modelMapper).map(dto, prototypeModel, MembershipCreationUseCase.MAP_NAME);
            inOrder.verify(membershipRepository).saveAndFlush(prototypeModel);
            inOrder.verify(modelMapper).map(savedModel, MembershipCreationResponseDTO.class);
        }
    }
}
