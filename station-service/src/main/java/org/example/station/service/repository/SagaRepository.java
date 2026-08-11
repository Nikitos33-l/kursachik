package org.example.station.service.repository;

import org.example.station.service.entity.SagaEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface SagaRepository extends JpaRepository<SagaEvent, UUID> {
}
