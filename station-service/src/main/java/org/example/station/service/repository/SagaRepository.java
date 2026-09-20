package org.example.station.service.repository;

import jakarta.persistence.LockModeType;
import org.example.station.service.entity.SagaEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SagaRepository extends JpaRepository<SagaEvent, UUID> {

    @Lock(value = LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM SagaEvent s WHERE s.id = :id")
    Optional<SagaEvent> findByIdWithLock(@Param("id")UUID id);
}
