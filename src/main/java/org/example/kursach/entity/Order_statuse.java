package org.example.kursach.entity;


import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@Entity
@NoArgsConstructor
@Table(name="order_statuses")
public class Order_statuse {
    @Id
    @Column(name="status_code")
    private String id;

    @Column(name="status_name")
    private String name;

}
