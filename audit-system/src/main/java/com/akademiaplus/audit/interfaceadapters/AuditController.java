/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.audit.interfaceadapters;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Placeholder controller for the audit subsystem.
 *
 * <p>Returns {@code 501 Not Implemented} for all audit endpoints.
 * This establishes the API contract so consumers can integrate early;
 * the implementation will follow in a future wave.
 */
@RestController
@RequestMapping("/v1/audit")
public class AuditController {

    public static final String NOT_IMPLEMENTED_CODE = "NOT_IMPLEMENTED";
    public static final String NOT_IMPLEMENTED_MSG = "Audit module is not yet implemented";

    /**
     * Placeholder for audit event query endpoint.
     *
     * @return 501 Not Implemented with structured error body
     */
    @GetMapping
    public ResponseEntity<Map<String, String>> getAuditEvents() {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED)
                .body(Map.of(
                        "code", NOT_IMPLEMENTED_CODE,
                        "message", NOT_IMPLEMENTED_MSG
                ));
    }
}
