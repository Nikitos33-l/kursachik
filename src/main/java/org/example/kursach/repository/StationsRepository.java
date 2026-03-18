package org.example.kursach.repository;

import org.example.kursach.entity.Stations;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StationsRepository extends JpaRepository<Stations,Long> {
}
