package org.example.station.service.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "outbox_events",
        indexes = @Index(name = "idx_outbox_status_created", columnList = "status, created_at"))
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class OutboxEvent {

    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    Long id;

    @Column(name = "event_id",nullable = false,unique = true)
    UUID eventId;

    @Column(name = "exchange",nullable = false)
    String exchange;

    @Column(name = "routing_key",nullable = false)
    String routingKey;

    @Column(name = "payload",nullable = false)
    String payload;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private OutboxStatus status = OutboxStatus.PENDING;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
