package com.peerlending.api;

import com.peerlending.api.dto.InvestRequest;
import com.peerlending.application.InvestmentService;
import com.peerlending.security.AuthPrincipal;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/loan-requests")
public class InvestmentController {

    private final InvestmentService investmentService;

    public InvestmentController(InvestmentService investmentService) {
        this.investmentService = investmentService;
    }

    @PostMapping("/{id}/invest")
    @PreAuthorize("hasRole('LENDER')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void invest(
            @AuthenticationPrincipal AuthPrincipal principal,
            @PathVariable Long id,
            @Valid @RequestBody InvestRequest request
    ) {
        investmentService.invest(id, principal.id(), request);
    }
}
