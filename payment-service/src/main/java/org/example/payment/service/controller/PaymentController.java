package org.example.payment.service.controller;

import org.example.payment.service.dto.PaymentResponse;
import org.example.payment.service.dto.yookassa.request.YooKassaWebhookNotification;
import org.example.payment.service.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payment")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/{orderId}")
    public PaymentResponse pay(@PathVariable Long orderId){
        return paymentService.initiatePayment(orderId);
    }

    @PostMapping("/webhook/yookassa")
    public ResponseEntity<Void> handleYooKassaWebhook(@RequestBody YooKassaWebhookNotification notification){
        paymentService.handleWebhook(notification);
        return ResponseEntity.ok().build();
    }

}
