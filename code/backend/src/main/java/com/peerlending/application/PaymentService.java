package com.peerlending.application;

import com.peerlending.common.exception.ConflictException;
import com.peerlending.common.exception.NotFoundException;
import com.peerlending.domain.InstallmentStatus;
import com.peerlending.domain.LoanStatus;
import com.peerlending.persistence.entity.LoanEntity;
import com.peerlending.persistence.entity.PaymentInstallmentEntity;
import com.peerlending.persistence.repository.LoanRepository;
import com.peerlending.persistence.repository.PaymentInstallmentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

@Service
public class PaymentService {

    private static final ZoneId REPUTATION_ZONE = ZoneId.of("Europe/Minsk");

    private final PaymentInstallmentRepository paymentInstallmentRepository;
    private final LoanRepository loanRepository;
    private final ReputationService reputationService;

    public PaymentService(
            PaymentInstallmentRepository paymentInstallmentRepository,
            LoanRepository loanRepository,
            ReputationService reputationService
    ) {
        this.paymentInstallmentRepository = paymentInstallmentRepository;
        this.loanRepository = loanRepository;
        this.reputationService = reputationService;
    }

    @Transactional
    public void payInstallment(Long loanId, Long installmentId, Long borrowerId) {
        LoanEntity loan = loanRepository.findById(loanId).orElseThrow(() -> new NotFoundException("Loan not found"));
        if (!loan.getLoanRequest().getBorrower().getId().equals(borrowerId)) {
            throw new com.peerlending.common.exception.ForbiddenOperationException("Not your loan");
        }
        if (loan.getStatus() != LoanStatus.ACTIVE) {
            throw new ConflictException("Loan is not active");
        }
        PaymentInstallmentEntity inst = paymentInstallmentRepository.findByIdAndLoanId(installmentId, loanId)
                .orElseThrow(() -> new NotFoundException("Installment not found"));
        if (inst.getStatus() != InstallmentStatus.SCHEDULED) {
            throw new ConflictException("Installment already processed");
        }
        Instant paidAt = Instant.now();
        inst.setStatus(InstallmentStatus.PAID);
        inst.setPaidAt(paidAt);
        paymentInstallmentRepository.save(inst);

        LocalDate due = inst.getDueDate();
        LocalDate paidDay = paidAt.atZone(REPUTATION_ZONE).toLocalDate();
        if (paidDay.isAfter(due)) {
            reputationService.adjustBorrowerReputation(borrowerId, -12);
        } else {
            reputationService.adjustBorrowerReputation(borrowerId, 8);
        }

        List<PaymentInstallmentEntity> remaining = paymentInstallmentRepository.findByLoanIdOrderByInstallmentNumber(loanId);
        boolean allPaid = remaining.stream().allMatch(i -> i.getStatus() == InstallmentStatus.PAID);
        if (allPaid) {
            loan.setStatus(LoanStatus.COMPLETED);
            loanRepository.save(loan);
            reputationService.adjustBorrowerReputation(borrowerId, 25);
        }
    }
}
