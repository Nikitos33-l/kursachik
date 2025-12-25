package org.example.kursach.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@Entity
@Table(name = "venicles")
@NoArgsConstructor
public class Venicle {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="venicle_id")
    private Long id;
    @Column(name="make")
    private String make;
    @Column(name="model")
    private String model;
    @Column(name = "number",unique = true)
    private String number;
}
