package com.example.payment.service.repository;

import com.example.payment.service.entity.OutboxEvent;
import com.example.payment.service.entity.OutboxStatus;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OutboxEventRepository extends JpaRepository<OutboxEvent,Long> {
    List<OutboxEvent> findAllByStatusOrderByCreatedAtAsc(OutboxStatus status, Limit limit);
}
