package com.example.payment.service.service;

import com.example.payment.service.dto.PaymentDetails;
import com.example.payment.service.dto.RemotePaymentResult;

public interface ExternalPaymentService {
    RemotePaymentResult initiatePay(PaymentDetails paymentDetails);
}
