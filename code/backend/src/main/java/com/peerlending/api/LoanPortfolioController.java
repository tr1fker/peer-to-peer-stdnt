package com.peerlending.api;

import com.peerlending.api.dto.GuaranteeDto;
import com.peerlending.api.dto.InstallmentDto;
import com.peerlending.api.dto.LoanDto;
import com.peerlending.api.dto.AddCoSignerRequest;
import com.peerlending.application.GuaranteeService;
import com.peerlending.application.LoanQueryService;
import com.peerlending.application.PaymentService;
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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class LoanPortfolioController {

    private final LoanQueryService loanQueryService;
    private final PaymentService paymentService;
    private final GuaranteeService guaranteeService;

    public LoanPortfolioController(
            LoanQueryService loanQueryService,
            PaymentService paymentService,
            GuaranteeService guaranteeService
    ) {
        this.loanQueryService = loanQueryService;
        this.paymentService = paymentService;
        this.guaranteeService = guaranteeService;
    }

    @GetMapping("/me/loans/borrower")
    @PreAuthorize("isAuthenticated()")
    public List<LoanDto> myBorrowed(@AuthenticationPrincipal AuthPrincipal principal) {
        return loanQueryService.myLoansAsBorrower(principal.id());
    }

    @GetMapping("/me/loans/lender")
    @PreAuthorize("isAuthenticated()")
    public List<LoanDto> myLent(@AuthenticationPrincipal AuthPrincipal principal) {
        return loanQueryService.myLoansAsLender(principal.id());
    }

    @GetMapping("/loans/{loanId}/installments")
    @PreAuthorize("isAuthenticated()")
    public List<InstallmentDto> installments(
            @AuthenticationPrincipal AuthPrincipal principal,
            @PathVariable Long loanId
    ) {
        return loanQueryService.installmentsForLoan(loanId, principal.id());
    }

    @GetMapping("/loans/{loanId}/guarantees")
    public List<GuaranteeDto> guarantees(@PathVariable Long loanId) {
        return loanQueryService.guaranteesForLoan(loanId);
    }

    @PostMapping("/loans/{loanId}/guarantees/co-signer")
    @PreAuthorize("isAuthenticated()")
    public GuaranteeDto addCoSigner(
            @AuthenticationPrincipal AuthPrincipal principal,
            @PathVariable Long loanId,
            @Valid @RequestBody AddCoSignerRequest request
    ) {
        return guaranteeService.addCoSigner(loanId, principal.id(), request);
    }

    @PostMapping("/loans/{loanId}/installments/{installmentId}/pay")
    @PreAuthorize("isAuthenticated()")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void pay(
            @AuthenticationPrincipal AuthPrincipal principal,
            @PathVariable Long loanId,
            @PathVariable Long installmentId
    ) {
        paymentService.payInstallment(loanId, installmentId, principal.id());
    }
}
