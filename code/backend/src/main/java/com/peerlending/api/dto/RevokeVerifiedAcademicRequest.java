package com.peerlending.api.dto;

import jakarta.validation.constraints.Size;

public record RevokeVerifiedAcademicRequest(
        @Size(max = 2000) String reason
) {
}
