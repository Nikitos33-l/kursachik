package org.example.kursach.dto;


import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.example.kursach.entity.Vehicle;

import java.util.List;

@Setter
@Getter
@AllArgsConstructor
public class ReguestOrderDTO {
    @NotNull
    private Vehicle vehicle;
    @NotNull
    private List<Long> serviceId;

    @NotNull
    private Long stationId;

}
