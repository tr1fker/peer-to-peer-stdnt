package com.peerlending.api.dto;

import com.peerlending.domain.GuaranteeStatus;

import java.math.BigDecimal;

public record CoSignerInvitationDto(
        long guaranteeId,
        long loanId,
        long loanRequestId,
        String borrowerEmail,
        String borrowerFullName,
        BigDecimal loanPrincipal,
        BigDecimal coverageAmount,
        GuaranteeStatus status
) {
}
