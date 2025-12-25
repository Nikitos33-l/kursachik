package org.example.kursach.repository;


import org.example.kursach.entity.Order;
import org.example.kursach.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order,Long> {
    List<Order> findAllByClient(User client);
}
