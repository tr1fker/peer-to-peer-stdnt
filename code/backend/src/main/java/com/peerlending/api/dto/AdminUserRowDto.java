package com.peerlending.api.dto;

import java.time.Instant;
import java.util.Set;

public record AdminUserRowDto(
        Long id,
        String email,
        boolean enabled,
        boolean blocked,
        String blockedReason,
        Instant blockedAt,
        Set<String> roles,
        String fullName
) {
}
