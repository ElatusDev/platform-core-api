/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.customer.tutor.interfaceadapters;

import com.akademiaplus.customer.tutor.usecases.GetAllTutorsUseCase;
import com.akademiaplus.customer.tutor.usecases.GetTutorByIdUseCase;
import com.akademiaplus.customer.tutor.usecases.TutorCreationUseCase;
import openapi.akademiaplus.domain.user.management.api.TutorsApi;
import openapi.akademiaplus.domain.user.management.dto.GetAllTutors200ResponseDTO;
import openapi.akademiaplus.domain.user.management.dto.GetTutorResponseDTO;
import openapi.akademiaplus.domain.user.management.dto.TutorCreationRequestDTO;
import openapi.akademiaplus.domain.user.management.dto.TutorCreationResponseDTO;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST controller for tutor management operations.
 *
 * @author ElatusDev
 * @since 1.0
 */
@RestController
@RequestMapping("/v1/user-management")
public class TutorController implements TutorsApi {

    private final TutorCreationUseCase tutorCreationUseCase;
    private final GetAllTutorsUseCase getAllTutorsUseCase;
    private final GetTutorByIdUseCase getTutorByIdUseCase;

    public TutorController(TutorCreationUseCase tutorCreationUseCase,
                           GetAllTutorsUseCase getAllTutorsUseCase,
                           GetTutorByIdUseCase getTutorByIdUseCase) {
        this.tutorCreationUseCase = tutorCreationUseCase;
        this.getAllTutorsUseCase = getAllTutorsUseCase;
        this.getTutorByIdUseCase = getTutorByIdUseCase;
    }

    @Override
    public ResponseEntity<TutorCreationResponseDTO> createTutor(
            TutorCreationRequestDTO tutorCreationRequestDTO) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(tutorCreationUseCase.create(tutorCreationRequestDTO));
    }

    @Override
    public ResponseEntity<GetAllTutors200ResponseDTO> getAllTutors(Integer page, Integer size) {
        List<GetTutorResponseDTO> tutors = getAllTutorsUseCase.getAll();
        GetAllTutors200ResponseDTO response = new GetAllTutors200ResponseDTO();
        response.setData(tutors);
        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<GetTutorResponseDTO> getTutor(Long tutorId) {
        return ResponseEntity.ok(getTutorByIdUseCase.get(tutorId));
    }
}
