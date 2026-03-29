package org.example.kursach.dto;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.example.kursach.entity.Service;
import org.example.kursach.entity.Vehicle;

import java.util.List;

@Setter
@Getter
@AllArgsConstructor
public class OrderDTO {
    private Long id;
    private Vehicle vehicle;
    private String status;
    private UserDTO client;
    private List<UserDTO> workers;
    private List<Service> services;

}
