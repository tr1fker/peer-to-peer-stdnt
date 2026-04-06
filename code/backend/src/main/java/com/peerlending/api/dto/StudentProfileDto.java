package com.peerlending.api.dto;

import com.peerlending.domain.ProfileVerificationStatus;

public record StudentProfileDto(
        String fullName,
        String university,
        String studentGroup,
        ProfileVerificationStatus verificationStatus,
        int reputationPoints
) {
}
