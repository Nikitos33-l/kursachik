package org.example.kursach.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@RequiredArgsConstructor
public class Update_orderDTO {
    @NotNull
    private final List<Long> workers_id;
    @NotNull
    private final String status_id;


}
