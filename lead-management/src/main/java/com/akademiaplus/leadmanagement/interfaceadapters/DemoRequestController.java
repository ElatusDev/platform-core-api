/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.leadmanagement.interfaceadapters;

import com.akademiaplus.leadmanagement.usecases.DeleteDemoRequestUseCase;
import com.akademiaplus.leadmanagement.usecases.DemoRequestCreationUseCase;
import com.akademiaplus.leadmanagement.usecases.GetAllDemoRequestsUseCase;
import com.akademiaplus.leadmanagement.usecases.GetDemoRequestByIdUseCase;
import lombok.RequiredArgsConstructor;
import openapi.akademiaplus.domain.lead.management.api.LeadManagementApi;
import openapi.akademiaplus.domain.lead.management.dto.DemoRequestCreationRequestDTO;
import openapi.akademiaplus.domain.lead.management.dto.DemoRequestCreationResponseDTO;
import openapi.akademiaplus.domain.lead.management.dto.GetAllDemoRequests200ResponseDTO;
import openapi.akademiaplus.domain.lead.management.dto.GetDemoRequestResponseDTO;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for demo request CRUD operations.
 * <p>
 * Implements the OpenAPI-generated {@link LeadManagementApi} interface,
 * delegating to domain use cases. The POST endpoint is public;
 * GET and DELETE endpoints require authentication.
 *
 * @author ElatusDev
 * @since 1.0
 */
@RestController
@RequestMapping("/v1")
@RequiredArgsConstructor
public class DemoRequestController implements LeadManagementApi {

    private final DemoRequestCreationUseCase demoRequestCreationUseCase;
    private final GetDemoRequestByIdUseCase getDemoRequestByIdUseCase;
    private final GetAllDemoRequestsUseCase getAllDemoRequestsUseCase;
    private final DeleteDemoRequestUseCase deleteDemoRequestUseCase;

    /** {@inheritDoc} */
    @Override
    public ResponseEntity<DemoRequestCreationResponseDTO> createDemoRequest(
            DemoRequestCreationRequestDTO demoRequestCreationRequestDTO) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(demoRequestCreationUseCase.create(demoRequestCreationRequestDTO));
    }

    /** {@inheritDoc} */
    @Override
    public ResponseEntity<GetDemoRequestResponseDTO> getDemoRequestById(Long id) {
        return ResponseEntity.ok(getDemoRequestByIdUseCase.get(id));
    }

    /** {@inheritDoc} */
    @Override
    public ResponseEntity<GetAllDemoRequests200ResponseDTO> getAllDemoRequests() {
        GetAllDemoRequests200ResponseDTO response = new GetAllDemoRequests200ResponseDTO();
        response.setDemoRequests(getAllDemoRequestsUseCase.getAll());
        return ResponseEntity.ok(response);
    }

    /** {@inheritDoc} */
    @Override
    public ResponseEntity<Void> deleteDemoRequest(Long id) {
        deleteDemoRequestUseCase.delete(id);
        return ResponseEntity.noContent().build();
    }
}
