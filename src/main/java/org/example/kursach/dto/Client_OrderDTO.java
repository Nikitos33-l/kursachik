package org.example.kursach.dto;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.example.kursach.entity.Service;
import org.example.kursach.entity.Venicle;
import org.example.kursach.entity.Venicle;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
public class Client_OrderDTO {
    private Venicle venicle;
    private List<Service> services;
    private String status;

}
