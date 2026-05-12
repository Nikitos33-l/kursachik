package org.example.station.service.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

@Setter
@Getter
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "station")
public class Station {
    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name")
    private String name;

    @Column(name = "latitude",precision = 9,scale = 6)
    private BigDecimal latitude;

    @Column(name = "longitude",precision = 9, scale = 6)
    private BigDecimal 	longitude;

    @Column(name = "address_text")
    private String addressText;

    @ElementCollection
    @CollectionTable(name = "station_orders",joinColumns = @JoinColumn(name = "station_id"))
    @Column(name = "order_id")
    Set<Long> orderIds;

    @OneToMany
    @JoinColumn(name =  "station_id")
    private List<Service> services;


}
