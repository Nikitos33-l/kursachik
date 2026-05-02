package org.example.user.api.responceDto;

import java.util.Map;

public record ValidationResponse(
        boolean exists,
        Map<Long,String> emails
) {
}
