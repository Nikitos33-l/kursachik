package org.example.kursach.dto;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.example.kursach.entity.Service;
import org.example.kursach.entity.Venicle;

import java.util.List;

@Setter
@Getter
@AllArgsConstructor
public class OrderDTO {
    private Long id;
    private Venicle venicle;
    private String status;
    private UserDTO client;
    private List<UserDTO> workers;
    private List<Service> services;

}
