package org.example.user.api.responceDto;

import java.util.Map;
import java.util.UUID;

public record ValidationResponse(
        boolean exists,
        Map<UUID,String> emails
) {
}
