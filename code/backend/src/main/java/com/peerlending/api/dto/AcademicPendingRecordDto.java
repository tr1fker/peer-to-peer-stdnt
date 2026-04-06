package com.peerlending.api.dto;

import java.math.BigDecimal;
import java.time.Instant;

/** Pending academic submission + student identity for admin review. */
public record AcademicPendingRecordDto(
        Long recordId,
        BigDecimal gradeAverage,
        String description,
        Instant submittedAt,
        Long userId,
        String userEmail,
        String profileFullName,
        String university,
        String studentGroup,
        String profileVerificationStatus
) {
}
