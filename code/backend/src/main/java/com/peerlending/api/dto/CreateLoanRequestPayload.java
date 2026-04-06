package com.peerlending.api.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record CreateLoanRequestPayload(
        @NotNull @DecimalMin("1000.00") BigDecimal amount,
        @NotNull @Min(1) Integer termMonths,
        String purpose,
        @NotNull @DecimalMin("0.0") @DecimalMax("100.0") BigDecimal interestRatePercent
) {
}
