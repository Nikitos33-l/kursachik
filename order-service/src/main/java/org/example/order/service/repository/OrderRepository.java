package org.example.order.service.repository;

import org.example.order.service.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface OrderRepository extends JpaRepository<Order,Long> {

    @Query("SELECT DISTINCT o FROM Order o LEFT JOIN FETCH o.orderItems WHERE o.stationId =:stationId")
    List<Order> findAllByStationId(@Param("stationId")Long stationId);

    @Query("SELECT DISTINCT o FROM Order o LEFT JOIN FETCH o.orderItems WHERE o.clientId=:clientId")
    List<Order> findAllByClientId(@Param("clientId") UUID clientId);

    @Query("SELECT DISTINCT o FROM Order o LEFT JOIN FETCH o.orderItems" +
            " JOIN o.workerIds w WHERE w=:workerId")
    List<Order> findAllByWorkerId(@Param("workerId") UUID workerId);

    List<Order> deleteAllByClientId(UUID clientId);

    List<Order> deleteAllByStationId(Long stationId);

}
