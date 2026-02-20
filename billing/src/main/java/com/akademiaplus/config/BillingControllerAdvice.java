/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.config;

import com.akademiaplus.membership.interfaceadapters.MembershipAdultStudentController;
import com.akademiaplus.membership.interfaceadapters.MembershipController;
import com.akademiaplus.membership.interfaceadapters.MembershipTutorController;
import com.akademiaplus.payment.interfaceadapters.PaymentAdultStudentController;
import com.akademiaplus.payment.interfaceadapters.PaymentTutorController;
import com.akademiaplus.payroll.interfaceadapters.CompensationController;
import com.akademiaplus.utilities.MessageService;
import com.akademiaplus.utilities.web.BaseControllerAdvice;
import org.springframework.web.bind.annotation.ControllerAdvice;

/**
 * Controller advice for billing module exception handling.
 * Extends {@link BaseControllerAdvice} to inherit generic exception handling capabilities
 * including {@link com.akademiaplus.utilities.exceptions.EntityNotFoundException}.
 *
 * @author ElatusDev
 * @since 1.0
 */
@ControllerAdvice(basePackageClasses = {MembershipController.class,
        MembershipAdultStudentController.class, MembershipTutorController.class,
        PaymentAdultStudentController.class, PaymentTutorController.class,
        CompensationController.class})
public class BillingControllerAdvice extends BaseControllerAdvice {

    /**
     * Constructs a new BillingControllerAdvice with the required message service.
     *
     * @param messageService the service for retrieving localized messages
     */
    public BillingControllerAdvice(MessageService messageService) {
        super(messageService);
    }
}
