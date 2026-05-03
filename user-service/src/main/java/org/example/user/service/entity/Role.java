package org.example.user.service.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


@Setter
@Getter
@Entity
@NoArgsConstructor
@Table(name="roles")
public class Role {
    @Id
    @Column(name="role_id")
    private Long Id;
    @Column(name="role_name")
    private String name;
}
