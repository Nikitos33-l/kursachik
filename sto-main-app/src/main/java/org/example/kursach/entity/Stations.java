package org.example.kursach.entity;


import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Entity
@Table(name = "stations")
@NoArgsConstructor
@Getter
@Setter
public class Stations {
    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name")
    private String name;

    @Column(name = "latitude",precision = 9, scale = 6)
    private BigDecimal latitude;

    @Column(name = "longitude",precision = 9, scale = 6)
    private BigDecimal 	longitude;

    @Column(name = "address_text")
    private String addressText;

    @OneToMany(mappedBy = "station")
    private List<Order> orders;

    @OneToMany(mappedBy = "station")
    private List<Service> services;
}
