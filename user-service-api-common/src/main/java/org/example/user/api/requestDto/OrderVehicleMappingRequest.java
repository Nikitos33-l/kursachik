package org.example.user.api.requestDto;

public record OrderVehicleMappingRequest(
        Long orderId,
        Long vehicleId
) {
}
