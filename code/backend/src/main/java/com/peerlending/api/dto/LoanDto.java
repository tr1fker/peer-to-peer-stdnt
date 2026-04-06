package com.peerlending.api.dto;

import com.peerlending.domain.LoanStatus;

import java.math.BigDecimal;
import java.time.LocalDate;

public record LoanDto(
        Long id,
        Long loanRequestId,
        BigDecimal principal,
        BigDecimal interestRatePercent,
        LocalDate startDate,
        LocalDate endDate,
        LoanStatus status
) {
}
