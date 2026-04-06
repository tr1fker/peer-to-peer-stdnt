package com.peerlending.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @Email @NotBlank String email,
        @NotBlank String password
) {
    public LoginRequest {
        email = email == null ? "" : email.trim().toLowerCase();
    }
}
