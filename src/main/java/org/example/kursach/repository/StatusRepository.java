package org.example.kursach.repository;


import org.example.kursach.entity.Order_statuse;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StatusRepository extends JpaRepository<Order_statuse,String> {
    Optional<Order_statuse> findByName(String name);
}
