package com.peerlending.api.dto;

public record TokenResponse(
        String accessToken,
        String refreshToken,
        long expiresInSeconds
) {
}
