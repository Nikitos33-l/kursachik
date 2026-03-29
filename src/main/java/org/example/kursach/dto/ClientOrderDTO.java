package org.example.kursach.dto;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.example.kursach.entity.Service;
import org.example.kursach.entity.Vehicle;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
public class ClientOrderDTO {
    private Vehicle vehicle;
    private List<Service> services;
    private String status;

}
