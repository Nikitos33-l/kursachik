package org.example.station.service.repository;

import org.example.station.service.entity.Station;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Set;

@Repository
public interface StationRepository extends JpaRepository<Station,Long> {
    List<Station> findAllByIdIn(Collection<Long> ids);
}
