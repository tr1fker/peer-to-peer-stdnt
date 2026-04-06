package com.peerlending.api.dto;

import com.peerlending.domain.LoanRequestStatus;

import java.math.BigDecimal;
import java.time.Instant;

/** One of the current user's investments with request funding context. */
public record MyInvestmentDto(
        Long investmentId,
        BigDecimal amount,
        Long loanRequestId,
        LoanRequestStatus loanRequestStatus,
        BigDecimal loanRequestAmount,
        BigDecimal fundedAmount,
        Long loanId,
        Instant createdAt
) {
}
