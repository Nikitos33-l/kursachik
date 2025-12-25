package org.example.kursach.repository;


import org.example.kursach.entity.Order_statuse;
import org.springframework.data.jpa.repository.JpaRepository;

public interface Order_statusRepository extends JpaRepository<Order_statuse,String> {
}
