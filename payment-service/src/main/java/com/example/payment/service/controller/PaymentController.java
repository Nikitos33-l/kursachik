package com.example.payment.service.controller;

import com.example.payment.service.dto.PaymentResponse;
import com.example.payment.service.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/payment")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/{orderId}")
    public PaymentResponse pay(@PathVariable Long orderId){
        return paymentService.initiatePayment(orderId);
    }

}
