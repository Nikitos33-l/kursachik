package org.example.kursach.repository;


import org.example.kursach.entity.Venicle;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VehicleRepository extends JpaRepository<Venicle,Long> {
    boolean existsByMakeAndModel(String make,String model);

    Venicle findByMakeAndModel(String make, String model);
}
