/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.config;

import com.akademiaplus.notification.interfaceadapters.EmailController;
import com.akademiaplus.notification.interfaceadapters.NotificationController;
import com.akademiaplus.utilities.MessageService;
import com.akademiaplus.utilities.web.BaseControllerAdvice;
import org.springframework.web.bind.annotation.ControllerAdvice;

/**
 * Controller advice for notification system exception handling.
 * Extends {@link BaseControllerAdvice} to provide centralized exception handling
 * for the notification system module.
 *
 * @author ElatusDev
 * @since 1.0
 */
@ControllerAdvice(basePackageClasses = {NotificationController.class, EmailController.class})
public class NotificationControllerAdvice extends BaseControllerAdvice {

    /**
     * Constructs a new NotificationControllerAdvice with the required message service.
     *
     * @param messageService the service for retrieving localized messages
     */
    public NotificationControllerAdvice(MessageService messageService) {
        super(messageService);
    }
}
