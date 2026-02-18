/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.interfaceadapters;

import com.akademiaplus.users.usecases.LoadPeopleMockDataUseCase;
import com.akademiaplus.users.usecases.LoadTenantMockDataUseCase;
import openapi.akademiaplus.domain.mock.data.system.api.MockDataApi;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/infra")
public class MockDataController implements MockDataApi {
    private final LoadTenantMockDataUseCase loadTenantMockDataUseCase;
    private final LoadPeopleMockDataUseCase loadPeopleMockDataUseCase;

    public MockDataController(LoadTenantMockDataUseCase loadTenantMockDataUseCase,
                              LoadPeopleMockDataUseCase loadPeopleMockDataUseCase) {
        this.loadTenantMockDataUseCase = loadTenantMockDataUseCase;
        this.loadPeopleMockDataUseCase = loadPeopleMockDataUseCase;
    }

    /**
     * Generates mock data for all entity types.
     * <p>
     * Orchestration respects FK constraints:
     * <ol>
     *   <li>Cleanup leaf-to-root: people first, then tenants</li>
     *   <li>Load root-to-leaf: tenants first, then people</li>
     * </ol>
     */
    @Override
    public ResponseEntity<String> generateAllMockData(Integer count) {
        loadPeopleMockDataUseCase.cleanUp();
        loadTenantMockDataUseCase.clean();

        loadTenantMockDataUseCase.load(count);
        loadPeopleMockDataUseCase.load(count);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body("Mock data generated: " + count + " records per entity type.");
    }
}
