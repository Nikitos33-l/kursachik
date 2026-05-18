package org.example.user.service.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.List;
import java.util.UUID;

@Setter
@Builder
@AllArgsConstructor
@Getter
@Entity
@NoArgsConstructor
@Table(name = "users")
public class User {
    @Id
    @Column(name="user_id")
    private UUID id;

    @Column (name="password_hash")
    private String password;

    @Column(name = "email",unique = true)
    private String email;

    @Column(name ="user_name")
    private String name;

    @ManyToOne
    @JoinColumn(name="role_id")
    private Role role;

    @OneToMany(cascade = CascadeType.REMOVE)
    @JoinColumn(name = "user_id")
    private List<Vehicle> vehicles;

    @Column(name = "station_id")
    private Long workplaceId;
}
