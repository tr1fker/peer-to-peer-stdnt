package com.peerlending.api.dto;

import com.peerlending.domain.GuaranteeStatus;
import com.peerlending.domain.GuaranteeType;

import java.math.BigDecimal;

public record GuaranteeDto(
        Long id,
        Long loanId,
        Long guarantorUserId,
        GuaranteeType guaranteeType,
        BigDecimal coverageAmount,
        GuaranteeStatus status
) {
}
