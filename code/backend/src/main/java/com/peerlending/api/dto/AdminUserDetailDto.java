package com.peerlending.api.dto;

import java.time.Instant;
import java.util.Set;

public record AdminUserDetailDto(
        Long id,
        String email,
        boolean enabled,
        boolean emailVerified,
        Instant createdAt,
        boolean blocked,
        String blockedReason,
        Instant blockedAt,
        Set<String> roles,
        String fullName,
        String university,
        String studentGroup,
        String profileVerificationStatus,
        long verifiedAcademicRecordsCount,
        long pendingAcademicRecordsCount,
        long openLoanRequestsCount,
        long activeLoansCount
) {
}
