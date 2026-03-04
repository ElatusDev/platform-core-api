package com.akademiaplus.payment.usecases;

import com.mercadopago.MercadoPagoConfig;
import com.mercadopago.client.order.*;
import com.mercadopago.core.MPRequestOptions;
import com.mercadopago.exceptions.MPApiException;
import com.mercadopago.resources.order.Order;
import com.mercadopago.exceptions.MPException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;


@Slf4j
@Service
public class RequestPaymentUseCase {

    public RequestPaymentUseCase(@Value("${mp.access.token}") String accessToken) {
        MercadoPagoConfig.setAccessToken(accessToken);
    }

    public void request() {
        OrderClient client = new OrderClient();
        OrderPaymentRequest payment = OrderPaymentRequest.builder()
                .amount("10.00")
                .paymentMethod(OrderPaymentMethodRequest.builder()
                        .id("master")
                        .type("credit_card")
                        .token("TEST-bf06d974-5825-4fb8-bb01-9a3325fd54b6")
                        .installments(1)
                        .build())
                .build();
        List<OrderPaymentRequest> payments = new ArrayList<>();
        payments.add(payment);

        OrderCreateRequest request = OrderCreateRequest.builder()
                .type("online")
                .totalAmount("10.00")
                .externalReference("ext_ref")
                .payer(OrderPayerRequest.builder().email("david.martinezenciso@gmail.com").build())
                .transactions(OrderTransactionRequest.builder()
                        .payments(payments)
                        .build())
                .build();

        Map<String, String> headers =  new HashMap<>();
        String idempotencyKey = UUID.randomUUID().toString();

        headers.put("X-Idempotency-Key", idempotencyKey);

        MPRequestOptions requestOptions = MPRequestOptions.builder()
                .customHeaders(headers)
                .build();
        try {
            Order order = client.create(request, requestOptions);
            log.info("Order created: {}", order.getResponse().getContent());
        } catch (MPApiException | MPException e) {
            log.error("Error creating order: {}", e.getMessage());

        }
    }

}
