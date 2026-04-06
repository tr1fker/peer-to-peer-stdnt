package com.peerlending.application;

import com.peerlending.application.loan.InstallmentScheduleFactory;
import com.peerlending.application.loan.LoanFundedEvent;
import com.peerlending.domain.GuaranteeStatus;
import com.peerlending.domain.GuaranteeType;
import com.peerlending.domain.LoanRequestStatus;
import com.peerlending.domain.LoanStatus;
import com.peerlending.persistence.entity.LoanEntity;
import com.peerlending.persistence.entity.LoanGuaranteeEntity;
import com.peerlending.persistence.entity.LoanRequestEntity;
import com.peerlending.persistence.entity.PaymentInstallmentEntity;
import com.peerlending.persistence.repository.InvestmentRepository;
import com.peerlending.persistence.repository.LoanGuaranteeRepository;
import com.peerlending.persistence.repository.LoanRepository;
import com.peerlending.persistence.repository.LoanRequestRepository;
import com.peerlending.persistence.repository.PaymentInstallmentRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

@Service
public class LoanActivationService {

    private final LoanRequestRepository loanRequestRepository;
    private final LoanRepository loanRepository;
    private final InvestmentRepository investmentRepository;
    private final LoanGuaranteeRepository loanGuaranteeRepository;
    private final PaymentInstallmentRepository paymentInstallmentRepository;
    private final InstallmentScheduleFactory installmentScheduleFactory;
    private final ApplicationEventPublisher eventPublisher;

    public LoanActivationService(
            LoanRequestRepository loanRequestRepository,
            LoanRepository loanRepository,
            InvestmentRepository investmentRepository,
            LoanGuaranteeRepository loanGuaranteeRepository,
            PaymentInstallmentRepository paymentInstallmentRepository,
            InstallmentScheduleFactory installmentScheduleFactory,
            ApplicationEventPublisher eventPublisher
    ) {
        this.loanRequestRepository = loanRequestRepository;
        this.loanRepository = loanRepository;
        this.investmentRepository = investmentRepository;
        this.loanGuaranteeRepository = loanGuaranteeRepository;
        this.paymentInstallmentRepository = paymentInstallmentRepository;
        this.installmentScheduleFactory = installmentScheduleFactory;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public void activateIfFullyFunded(Long loanRequestId) {
        LoanRequestEntity request = loanRequestRepository.findById(loanRequestId)
                .orElseThrow(() -> new com.peerlending.common.exception.NotFoundException("Loan request not found"));
        if (request.getStatus() != LoanRequestStatus.OPEN) {
            return;
        }
        if (loanRepository.findByLoanRequest_Id(loanRequestId).isPresent()) {
            return;
        }
        BigDecimal sum = investmentRepository.sumAmountByLoanRequestId(loanRequestId);
        if (sum.compareTo(request.getAmount()) < 0) {
            return;
        }

        request.setStatus(LoanRequestStatus.FUNDED);
        loanRequestRepository.save(request);

        LoanEntity loan = new LoanEntity();
        loan.setLoanRequest(request);
        loan.setPrincipal(request.getAmount());
        loan.setInterestRatePercent(request.getInterestRatePercent());
        LocalDate start = LocalDate.now();
        loan.setStartDate(start);
        loan.setEndDate(start.plusMonths(request.getTermMonths()));
        loan.setStatus(LoanStatus.ACTIVE);
        loan = loanRepository.save(loan);

        LoanGuaranteeEntity platform = new LoanGuaranteeEntity();
        platform.setLoan(loan);
        platform.setGuarantor(null);
        platform.setGuaranteeType(GuaranteeType.PLATFORM_POOL);
        platform.setCoverageAmount(
                request.getAmount().multiply(BigDecimal.valueOf(0.2)).setScale(2, RoundingMode.HALF_UP)
        );
        platform.setStatus(GuaranteeStatus.ACTIVE);
        loanGuaranteeRepository.save(platform);

        List<PaymentInstallmentEntity> installments = installmentScheduleFactory.createEqualInstallments(loan);
        paymentInstallmentRepository.saveAll(installments);

        eventPublisher.publishEvent(new LoanFundedEvent(request.getId(), loan.getId()));
    }
}
