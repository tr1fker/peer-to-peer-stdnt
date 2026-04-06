package com.peerlending.api.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record InvestRequest(
        @NotNull @DecimalMin("0.01") BigDecimal amount
) {
}
