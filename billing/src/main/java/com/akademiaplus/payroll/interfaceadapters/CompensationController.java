/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.payroll.interfaceadapters;

import com.akademiaplus.payroll.usecases.CompensationCreationUseCase;
import com.akademiaplus.payroll.usecases.DeleteCompensationUseCase;
import com.akademiaplus.payroll.usecases.GetAllCompensationsUseCase;
import com.akademiaplus.payroll.usecases.GetCompensationByIdUseCase;
import openapi.akademiaplus.domain.billing.api.CompensationsApi;
import openapi.akademiaplus.domain.billing.dto.CompensationCreationRequestDTO;
import openapi.akademiaplus.domain.billing.dto.CompensationCreationResponseDTO;
import openapi.akademiaplus.domain.billing.dto.GetCompensationResponseDTO;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST controller for compensation management operations.
 *
 * @author ElatusDev
 * @since 1.0
 */
@RestController
@RequestMapping("/v1/billing")
public class CompensationController implements CompensationsApi {

    private final CompensationCreationUseCase compensationCreationUseCase;
    private final GetAllCompensationsUseCase getAllCompensationsUseCase;
    private final GetCompensationByIdUseCase getCompensationByIdUseCase;
    private final DeleteCompensationUseCase deleteCompensationUseCase;

    public CompensationController(CompensationCreationUseCase compensationCreationUseCase,
                                  GetAllCompensationsUseCase getAllCompensationsUseCase,
                                  GetCompensationByIdUseCase getCompensationByIdUseCase,
                                  DeleteCompensationUseCase deleteCompensationUseCase) {
        this.compensationCreationUseCase = compensationCreationUseCase;
        this.getAllCompensationsUseCase = getAllCompensationsUseCase;
        this.getCompensationByIdUseCase = getCompensationByIdUseCase;
        this.deleteCompensationUseCase = deleteCompensationUseCase;
    }

    @Override
    public ResponseEntity<CompensationCreationResponseDTO> createCompensation(
            CompensationCreationRequestDTO compensationCreationRequestDTO) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(compensationCreationUseCase.create(compensationCreationRequestDTO));
    }

    @Override
    public ResponseEntity<List<GetCompensationResponseDTO>> getCompensations() {
        return ResponseEntity.ok(getAllCompensationsUseCase.getAll());
    }

    @Override
    public ResponseEntity<GetCompensationResponseDTO> getCompensationById(Long compensationId) {
        return ResponseEntity.ok(getCompensationByIdUseCase.get(compensationId));
    }

    @Override
    public ResponseEntity<Void> deleteCompensation(Long compensationId) {
        deleteCompensationUseCase.delete(compensationId);
        return ResponseEntity.noContent().build();
    }
}
