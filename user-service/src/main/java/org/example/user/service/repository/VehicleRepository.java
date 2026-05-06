package org.example.user.service.repository;

import org.example.user.service.entity.Vehicle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;

@Repository
public interface VehicleRepository extends JpaRepository<Vehicle,Long> {

    List<Vehicle> findAllByIdIn(Set<Long> ids);
}
