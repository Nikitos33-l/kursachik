package org.example.station.service.repository;

import org.example.station.service.entity.Service;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface ServiceRepository extends JpaRepository<Service,Long> {
    List<Service> findAllByStation_id(Long id);

    List<Service> findAllByIdIn(Collection<Long> ids);

}
