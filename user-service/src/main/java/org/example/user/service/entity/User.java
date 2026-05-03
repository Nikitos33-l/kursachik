package org.example.user.service.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Setter
@Getter
@Entity
@NoArgsConstructor
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="user_id")
    private Long id;

    @Column (name="password_hash")
    private String password;

    @Column(name = "email")
    private String email;

    @Column(name ="user_name")
    private String name;

    @ManyToOne
    @JoinColumn(name="role_id")
    private Role role;

    @OneToMany
    @JoinColumn(name = "user_id")
    private List<Vehicle> vehicles;

    @Column(name = "station_id")
    private Long workplaceId;
}
