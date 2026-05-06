package org.example.user.api.responceDto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@AllArgsConstructor
@Getter
@Setter
@Builder
public class OrderInfoFromUserServiceDto {
    private UserDto client;
    private List<UserDto> workers;
    private VehicleDto vehicle;
}
