package org.example.kursach.repository;


import org.example.kursach.entity.Service;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ServiceRepository extends JpaRepository<Service,Long> {
    Service findByName(String name);
}
