package com.akademiaplus.payment.interfaceadapters;

import com.akademiaplus.payment.usecases.RequestPaymentUseCase;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/billing")
public class PaymentController {
    private final RequestPaymentUseCase requestPaymentUseCase;

    public PaymentController(RequestPaymentUseCase requestPaymentUseCase) {
        this.requestPaymentUseCase = requestPaymentUseCase;
    }

    @RequestMapping(method = RequestMethod.POST, value = "/payments")
    public ResponseEntity<String> requestPayment(String request) {
        requestPaymentUseCase.request();
        return ResponseEntity.ok("it worked!");
    }
 }
