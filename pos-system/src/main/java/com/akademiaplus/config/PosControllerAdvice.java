/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.config;

import com.akademiaplus.store.interfaceadapters.StoreProductController;
import com.akademiaplus.store.interfaceadapters.StoreTransactionController;
import com.akademiaplus.utilities.MessageService;
import com.akademiaplus.utilities.web.BaseControllerAdvice;
import org.springframework.web.bind.annotation.ControllerAdvice;

/**
 * Controller advice for POS system exception handling.
 * Extends {@link BaseControllerAdvice} to provide centralized exception handling
 * for the POS system module.
 *
 * @author ElatusDev
 * @since 1.0
 */
@ControllerAdvice(basePackageClasses = {StoreProductController.class, StoreTransactionController.class})
public class PosControllerAdvice extends BaseControllerAdvice {

    /**
     * Constructs a new PosControllerAdvice with the required message service.
     *
     * @param messageService the service for retrieving localized messages
     */
    public PosControllerAdvice(MessageService messageService) {
        super(messageService);
    }
}
