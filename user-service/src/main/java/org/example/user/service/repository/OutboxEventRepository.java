package org.example.user.service.repository;

import org.example.user.service.entity.OutboxEvent;
import org.example.user.service.entity.OutboxStatus;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OutboxEventRepository extends JpaRepository<OutboxEvent,Long> {
    List<OutboxEvent> findAllByStatusOrderByCreatedAtAsc(OutboxStatus status, Limit limit);
}
