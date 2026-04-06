package com.peerlending.api;

import com.peerlending.api.dto.CreateLoanRequestPayload;
import com.peerlending.api.dto.LoanRequestDto;
import com.peerlending.api.dto.PageResponse;
import com.peerlending.application.LoanRequestService;
import com.peerlending.domain.LoanRequestStatus;
import com.peerlending.security.AuthPrincipal;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/loan-requests")
public class LoanRequestController {

    private final LoanRequestService loanRequestService;

    public LoanRequestController(LoanRequestService loanRequestService) {
        this.loanRequestService = loanRequestService;
    }

    @GetMapping
    public PageResponse<LoanRequestDto> list(
            @RequestParam Optional<LoanRequestStatus> status,
            @RequestParam Optional<BigDecimal> minAmount,
            @RequestParam Optional<BigDecimal> maxAmount,
            @RequestParam Optional<Long> borrowerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortField,
            @RequestParam(defaultValue = "desc") String sortDir
    ) {
        return loanRequestService.search(status, minAmount, maxAmount, borrowerId, page, size, sortField, sortDir);
    }

    @GetMapping("/{id}")
    public LoanRequestDto get(@PathVariable Long id) {
        return loanRequestService.get(id);
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    @ResponseStatus(HttpStatus.CREATED)
    public LoanRequestDto create(
            @AuthenticationPrincipal AuthPrincipal principal,
            @Valid @RequestBody CreateLoanRequestPayload payload
    ) {
        return loanRequestService.create(principal.id(), payload);
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("isAuthenticated()")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancel(@AuthenticationPrincipal AuthPrincipal principal, @PathVariable Long id) {
        loanRequestService.cancel(id, principal.id());
    }
}
