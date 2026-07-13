package org.example.payment.service.service;

import org.example.payment.service.dto.PaymentDetails;
import org.example.payment.service.dto.RemotePaymentResult;

public interface ExternalPaymentService {
    RemotePaymentResult initiatePay(PaymentDetails paymentDetails);
}
