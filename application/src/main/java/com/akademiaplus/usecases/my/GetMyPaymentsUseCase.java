/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.usecases.my;

import com.akademiaplus.internal.interfaceadapters.UserContextHolder;
import com.akademiaplus.membership.interfaceadapters.PaymentAdultStudentRepository;
import openapi.akademiaplus.domain.my.dto.MyPaymentDTO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Retrieves payment history for the authenticated adult student.
 *
 * @author ElatusDev
 * @since 1.0
 */
@Service
public class GetMyPaymentsUseCase {

    private final UserContextHolder userContextHolder;
    private final PaymentAdultStudentRepository paymentAdultStudentRepository;

    /**
     * Constructs the use case with required dependencies.
     *
     * @param userContextHolder              the user context holder
     * @param paymentAdultStudentRepository  the payment repository
     */
    public GetMyPaymentsUseCase(UserContextHolder userContextHolder,
                                 PaymentAdultStudentRepository paymentAdultStudentRepository) {
        this.userContextHolder = userContextHolder;
        this.paymentAdultStudentRepository = paymentAdultStudentRepository;
    }

    /**
     * Retrieves all payments for the authenticated student.
     *
     * @return list of payment DTOs
     */
    @Transactional(readOnly = true)
    public List<MyPaymentDTO> execute() {
        Long profileId = userContextHolder.requireProfileId();

        return paymentAdultStudentRepository.findByAdultStudentId(profileId).stream()
                .map(p -> {
                    MyPaymentDTO dto = new MyPaymentDTO();
                    dto.setPaymentAdultStudentId(p.getPaymentAdultStudentId());
                    dto.setAmount(p.getAmount() != null ? p.getAmount().doubleValue() : null);
                    dto.setPaymentDate(p.getPaymentDate());
                    dto.setPaymentMethod(p.getPaymentMethod());
                    return dto;
                })
                .toList();
    }
}
