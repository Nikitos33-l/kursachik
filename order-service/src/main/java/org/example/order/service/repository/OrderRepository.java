package org.example.order.service.repository;

import org.example.order.service.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order,Long> {

    List<Order> findAllByStationId(Long stationId);

    List<Order> findAllByClientId(Long clientId);
}
