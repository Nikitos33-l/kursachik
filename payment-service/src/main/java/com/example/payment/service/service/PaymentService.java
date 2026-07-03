package com.example.payment.service.service;

import com.example.payment.service.dto.PaymentResponse;
import com.example.payment.service.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PaymentService {
    //TODO сделать клиента для сервиса заказов для получения суммы заказа по id
    private final PaymentRepository paymentRepository;

    public PaymentResponse initiatePayment() {
        //TODO написать метод для инициализации оплаты
        return null;
    }
}
