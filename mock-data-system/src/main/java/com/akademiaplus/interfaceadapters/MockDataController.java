/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.interfaceadapters;

import com.akademiaplus.users.usecases.LoadPeopleMockDataUseCase;
import openapi.akademiaplus.domain.mock.data.system.api.MockDataApi;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/infra")
public class MockDataController implements MockDataApi {
    private final LoadPeopleMockDataUseCase loadPeopleMockDataUseCase;

    public MockDataController(LoadPeopleMockDataUseCase loadPeopleMockDataUseCase) {
        this.loadPeopleMockDataUseCase = loadPeopleMockDataUseCase;
    }

    @Override
    public ResponseEntity<String> generateAllMockData() {
        loadPeopleMockDataUseCase.load();
        return ResponseEntity.status(HttpStatus.CREATED).body("mock data ready!");
    }




}
