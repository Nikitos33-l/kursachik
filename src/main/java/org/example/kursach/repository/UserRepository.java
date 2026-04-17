package org.example.kursach.repository;


import org.example.kursach.entity.Stations;
import org.example.kursach.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User,Long> {
    User findByEmail(String email);
    List<User> findByWorkplace(Stations workplace);
    boolean existsByEmail(String email);
    List<User> findAllByRole_Name(String roleName);
}
