package org.example.kursach.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.Hibernate;

import java.util.*;

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

    @OneToMany(mappedBy = "client",cascade = CascadeType.REMOVE)
    private List<Order> client_orders = new ArrayList<>();

    @ManyToMany(mappedBy = "workers")
    private Set<Order> worker_orders = new HashSet<>();

    @ManyToOne
    @JoinColumn(name = "station_id")
    private Stations workplace;


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;
        if (Hibernate.getClass(this) != Hibernate.getClass(o)) return false;
        User other = (User) o;
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
