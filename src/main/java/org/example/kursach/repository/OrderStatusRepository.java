package org.example.kursach.repository;


import org.example.kursach.entity.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderStatusRepository extends JpaRepository<OrderStatus,String> {
}
