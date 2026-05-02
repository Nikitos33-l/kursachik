package org.example.order.service.entity;


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
@Table(name="order_statuses")
public class OrderStatus {
    @Id
    @Column(name="status_code")
    private String id;

    @Column(name="status_name")
    private String name;

}
