package com.peerlending.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
        @NotBlank @Size(max = 255) String fullName,
        @Size(max = 255) String university,
        @Size(max = 128) String studentGroup
) {
}
