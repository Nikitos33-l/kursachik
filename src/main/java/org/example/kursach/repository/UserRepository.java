package org.example.kursach.repository;


import org.example.kursach.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User,Long> {
    User findByEmail(String email);
    Optional<User> findByEmailAndName(String email, String name);

    boolean existsByEmail(String email);
    List<User> findAllByRole_Name(String roleName);
}
