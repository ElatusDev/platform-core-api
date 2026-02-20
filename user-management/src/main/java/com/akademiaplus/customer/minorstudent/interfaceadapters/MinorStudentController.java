/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.customer.minorstudent.interfaceadapters;

import com.akademiaplus.customer.minorstudent.usecases.GetAllMinorStudentsUseCase;
import com.akademiaplus.customer.minorstudent.usecases.GetMinorStudentByIdUseCase;
import openapi.akademiaplus.domain.user.management.api.MinorStudentsApi;
import openapi.akademiaplus.domain.user.management.dto.GetAllMinorStudents200ResponseDTO;
import openapi.akademiaplus.domain.user.management.dto.GetMinorStudentResponseDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST controller for minor student management operations.
 *
 * @author ElatusDev
 * @since 1.0
 */
@RestController
@RequestMapping("/v1/user-management")
public class MinorStudentController implements MinorStudentsApi {

    private final GetAllMinorStudentsUseCase getAllMinorStudentsUseCase;
    private final GetMinorStudentByIdUseCase getMinorStudentByIdUseCase;

    public MinorStudentController(GetAllMinorStudentsUseCase getAllMinorStudentsUseCase,
                                  GetMinorStudentByIdUseCase getMinorStudentByIdUseCase) {
        this.getAllMinorStudentsUseCase = getAllMinorStudentsUseCase;
        this.getMinorStudentByIdUseCase = getMinorStudentByIdUseCase;
    }

    @Override
    public ResponseEntity<GetAllMinorStudents200ResponseDTO> getAllMinorStudents(
            Integer page, Integer size, Integer tutorId) {
        List<GetMinorStudentResponseDTO> minorStudents = getAllMinorStudentsUseCase.getAll();
        GetAllMinorStudents200ResponseDTO response = new GetAllMinorStudents200ResponseDTO();
        response.setData(minorStudents);
        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<GetMinorStudentResponseDTO> getMinorStudent(Long minorStudentId) {
        return ResponseEntity.ok(getMinorStudentByIdUseCase.get(minorStudentId));
    }
}
