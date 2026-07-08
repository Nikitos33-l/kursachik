package com.example.payment.service.repository;

import com.example.payment.service.entity.Payment;
import com.example.payment.service.entity.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentRepository extends JpaRepository<Payment,UUID> {
    Optional<Payment> findByPaymentStatusAndOrderId(PaymentStatus paymentStatus, Long orderId);

    Optional<Payment> findByExternalId(String externalId);
}
