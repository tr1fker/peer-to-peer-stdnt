package com.peerlending.application;

import com.peerlending.common.annotation.LoggedUseCase;
import com.peerlending.api.dto.InvestRequest;
import com.peerlending.common.exception.ConflictException;
import com.peerlending.common.exception.ForbiddenOperationException;
import com.peerlending.common.exception.NotFoundException;
import com.peerlending.domain.LoanRequestStatus;
import com.peerlending.persistence.entity.InvestmentEntity;
import com.peerlending.persistence.entity.LoanRequestEntity;
import com.peerlending.persistence.entity.UserEntity;
import com.peerlending.persistence.repository.InvestmentRepository;
import com.peerlending.persistence.repository.LoanRequestRepository;
import com.peerlending.persistence.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
public class InvestmentService {

    private final LoanRequestRepository loanRequestRepository;
    private final InvestmentRepository investmentRepository;
    private final UserRepository userRepository;
    private final LoanActivationService loanActivationService;

    public InvestmentService(
            LoanRequestRepository loanRequestRepository,
            InvestmentRepository investmentRepository,
            UserRepository userRepository,
            LoanActivationService loanActivationService
    ) {
        this.loanRequestRepository = loanRequestRepository;
        this.investmentRepository = investmentRepository;
        this.userRepository = userRepository;
        this.loanActivationService = loanActivationService;
    }

    @LoggedUseCase("invest-in-loan-request")
    @Transactional
    public void invest(Long loanRequestId, Long lenderId, InvestRequest request) {
        LoanRequestEntity lr = loanRequestRepository.findById(loanRequestId)
                .orElseThrow(() -> new NotFoundException("Loan request not found"));
        if (lr.getStatus() != LoanRequestStatus.OPEN) {
            throw new ConflictException("Loan request is not open for investment");
        }
        if (lr.getBorrower().getId().equals(lenderId)) {
            throw new ForbiddenOperationException("Cannot invest in your own request");
        }
        BigDecimal funded = investmentRepository.sumAmountByLoanRequestId(loanRequestId);
        BigDecimal remaining = lr.getAmount().subtract(funded);
        if (request.amount().compareTo(remaining) > 0) {
            throw new ForbiddenOperationException("Investment exceeds remaining amount: " + remaining);
        }
        if (request.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ForbiddenOperationException("Amount must be positive");
        }

        UserEntity lender = userRepository.getReferenceById(lenderId);
        InvestmentEntity inv = new InvestmentEntity();
        inv.setLoanRequest(lr);
        inv.setLender(lender);
        inv.setAmount(request.amount());
        investmentRepository.save(inv);

        loanActivationService.activateIfFullyFunded(loanRequestId);
    }
}
