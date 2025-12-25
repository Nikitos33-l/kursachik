package org.example.kursach.repository;


import org.example.kursach.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoleRepository extends JpaRepository<Role,Long> {
     Role findByName(String name);
}
