/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.config;

import com.akademiaplus.interfaceadapters.TenantController;
import com.akademiaplus.utilities.MessageService;
import com.akademiaplus.utilities.web.BaseControllerAdvice;
import org.springframework.web.bind.annotation.ControllerAdvice;

/**
 * Exception handler for tenant-management module REST controllers.
 *
 * <p>Extends {@link BaseControllerAdvice} to inherit standard exception
 * mappings (EntityNotFoundException → 404, etc.). Can be extended with
 * tenant-management-specific exception handlers if needed.
 *
 * @author ElatusDev
 * @since 1.0
 */
@ControllerAdvice(basePackageClasses = TenantController.class)
public class TenantManagementControllerAdvice extends BaseControllerAdvice {

    /**
     * Constructs the controller advice with message service for i18n.
     *
     * @param messageService the message service
     */
    public TenantManagementControllerAdvice(MessageService messageService) {
        super(messageService);
    }
}
