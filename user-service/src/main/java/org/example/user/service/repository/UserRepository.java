package org.example.user.service.repository;

import org.example.user.service.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;

@Repository
public interface UserRepository extends JpaRepository<User,Long> {
    @Query("SELECT u FROM User u LEFT JOIN FETCH u.role WHERE u.workplaceId =:stationId")
    List<User> findAll(@Param("stationId") Long stationId);

    List<User> findAllByRole_NameAndWorkplaceId(String role,Long stationId);

    boolean existsByEmail(String email);

    List<User> findAllByIdIn(Set<Long> ids);

    User findByEmail(String email);

    void deleteAllByWorkplaceId(Long workplaceId);

}
