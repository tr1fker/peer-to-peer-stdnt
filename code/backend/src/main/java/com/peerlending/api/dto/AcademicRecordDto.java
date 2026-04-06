package com.peerlending.api.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record AcademicRecordDto(
        Long id,
        BigDecimal gradeAverage,
        String description,
        Instant submittedAt,
        boolean verified,
        Instant verifiedAt,
        boolean rejected,
        Instant rejectedAt,
        String rejectionReason
) {
}
