package com.peerlending.api.dto;

import com.peerlending.domain.InstallmentStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record InstallmentDto(
        Long id,
        int installmentNumber,
        BigDecimal amountDue,
        LocalDate dueDate,
        InstallmentStatus status,
        Instant paidAt
) {
}
