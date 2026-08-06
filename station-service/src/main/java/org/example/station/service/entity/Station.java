package org.example.station.service.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLRestriction;

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
@SQLRestriction("status = 'ACTIVE'")
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

    @Column(name ="status")
    @Enumerated(value = EnumType.STRING)
    StationStatus status;

    @OneToMany(mappedBy = "station",cascade = CascadeType.REMOVE,orphanRemoval = true)
    private List<Service> services;

    public enum StationStatus {
        ACTIVE,
        DELETING
    };
}
