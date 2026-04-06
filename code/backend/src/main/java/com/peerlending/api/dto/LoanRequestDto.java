package com.peerlending.api.dto;

import com.peerlending.domain.LoanRequestStatus;

import java.math.BigDecimal;
import java.time.Instant;

public record LoanRequestDto(
        Long id,
        Long borrowerId,
        BigDecimal amount,
        int termMonths,
        String purpose,
        LoanRequestStatus status,
        BigDecimal interestRatePercent,
        Instant createdAt,
        BigDecimal fundedAmount,
        int borrowerReputationPoints
) {
}
