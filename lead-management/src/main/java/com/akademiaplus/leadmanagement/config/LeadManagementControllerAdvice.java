/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.leadmanagement.config;

import com.akademiaplus.leadmanagement.interfaceadapters.DemoRequestController;
import com.akademiaplus.utilities.MessageService;
import com.akademiaplus.utilities.web.BaseControllerAdvice;
import org.springframework.web.bind.annotation.ControllerAdvice;

/**
 * Exception handler for lead-management module REST controllers.
 * <p>
 * Extends {@link BaseControllerAdvice} to inherit standard exception
 * mappings (EntityNotFoundException → 404, DuplicateEntityException → 409, etc.).
 *
 * @author ElatusDev
 * @since 1.0
 */
@ControllerAdvice(basePackageClasses = DemoRequestController.class)
public class LeadManagementControllerAdvice extends BaseControllerAdvice {

    /**
     * Constructs the controller advice with message service for i18n.
     *
     * @param messageService the message service
     */
    public LeadManagementControllerAdvice(MessageService messageService) {
        super(messageService);
    }
}
