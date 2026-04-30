package org.example.kursach.repository;


import org.example.kursach.entity.Vehicle;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VehicleRepository extends JpaRepository<Vehicle,Long> {
    boolean existsByMakeAndModel(String make,String model);

    Vehicle findByMakeAndModel(String make, String model);
}
