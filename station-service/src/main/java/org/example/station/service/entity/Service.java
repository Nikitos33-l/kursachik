package org.example.station.service.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Setter
@Getter
@Entity
@Table(name = "service")
public class Service {
    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name")
    private String name;

    @Column(name="price")
    private BigDecimal price;

    @ManyToOne
    @JoinColumn(name = "station_id")
    private Station station;

}
