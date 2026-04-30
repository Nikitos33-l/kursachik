package org.example.kursach.repository;


import org.example.kursach.entity.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StatusRepository extends JpaRepository<OrderStatus,String> {
    Optional<OrderStatus> findByName(String name);
}
