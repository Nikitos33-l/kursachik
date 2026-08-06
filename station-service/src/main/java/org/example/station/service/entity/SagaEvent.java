package org.example.station.service.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "saga_events")
@Builder
@Setter
@Getter
public class SagaEvent {

    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "target_id",nullable = false)
    private Long targetId;

    @Column(name = "event_type",nullable = false)
    @Enumerated(value = EnumType.STRING)
    private TypeSagaEvent eventType;

    @Column(name = "saga_status",nullable = false)
    @Enumerated(value = EnumType.STRING)
    private SagaStatus sagaStatus;

    @Column(name = "payload")
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String,Object> payload = new HashMap<>();

    @Column(name = "created_at",nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

}
