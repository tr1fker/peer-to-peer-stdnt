package com.peerlending.application;

import com.peerlending.api.dto.GuaranteeDto;
import com.peerlending.api.dto.InstallmentDto;
import com.peerlending.api.dto.LoanDto;
import com.peerlending.api.dto.MyInvestmentDto;
import com.peerlending.common.exception.NotFoundException;
import com.peerlending.persistence.entity.InvestmentEntity;
import com.peerlending.persistence.entity.LoanEntity;
import com.peerlending.persistence.entity.LoanGuaranteeEntity;
import com.peerlending.persistence.entity.PaymentInstallmentEntity;
import com.peerlending.persistence.repository.InvestmentRepository;
import com.peerlending.persistence.repository.LoanGuaranteeRepository;
import com.peerlending.persistence.repository.LoanRepository;
import com.peerlending.persistence.repository.PaymentInstallmentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class LoanQueryService {

    private final LoanRepository loanRepository;
    private final InvestmentRepository investmentRepository;
    private final PaymentInstallmentRepository paymentInstallmentRepository;
    private final LoanGuaranteeRepository loanGuaranteeRepository;

    public LoanQueryService(
            LoanRepository loanRepository,
            InvestmentRepository investmentRepository,
            PaymentInstallmentRepository paymentInstallmentRepository,
            LoanGuaranteeRepository loanGuaranteeRepository
    ) {
        this.loanRepository = loanRepository;
        this.investmentRepository = investmentRepository;
        this.paymentInstallmentRepository = paymentInstallmentRepository;
        this.loanGuaranteeRepository = loanGuaranteeRepository;
    }

    @Transactional(readOnly = true)
    public List<LoanDto> myLoansAsBorrower(Long borrowerId) {
        return loanRepository.findByLoanRequest_Borrower_Id(borrowerId).stream()
                .map(LoanQueryService::toLoanDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<LoanDto> myLoansAsLender(Long lenderId) {
        return investmentRepository.findByLenderId(lenderId).stream()
                .map(inv -> loanRepository.findByLoanRequest_Id(inv.getLoanRequest().getId()))
                .flatMap(Optional::stream)
                .collect(Collectors.toMap(LoanEntity::getId, Function.identity(), (a, b) -> a))
                .values()
                .stream()
                .map(LoanQueryService::toLoanDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<InstallmentDto> installmentsForLoan(Long loanId, Long viewerUserId) {
        LoanEntity loan = loanRepository.findById(loanId).orElseThrow(() -> new NotFoundException("Loan not found"));
        Long requestId = loan.getLoanRequest().getId();
        boolean isBorrower = loan.getLoanRequest().getBorrower().getId().equals(viewerUserId);
        boolean isInvestor = investmentRepository.existsByLender_IdAndLoanRequest_Id(viewerUserId, requestId);
        if (!isBorrower && !isInvestor) {
            throw new com.peerlending.common.exception.ForbiddenOperationException("Нет доступа к этому займу");
        }
        return paymentInstallmentRepository.findByLoanIdOrderByInstallmentNumber(loanId).stream()
                .map(LoanQueryService::toInstallmentDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<MyInvestmentDto> myInvestments(Long lenderId) {
        return investmentRepository.findByLenderId(lenderId).stream()
                .sorted(Comparator.comparing(InvestmentEntity::getCreatedAt).reversed())
                .map(inv -> {
                    var lr = inv.getLoanRequest();
                    BigDecimal funded = investmentRepository.sumAmountByLoanRequestId(lr.getId());
                    Long loanId = loanRepository.findByLoanRequest_Id(lr.getId()).map(LoanEntity::getId).orElse(null);
                    return new MyInvestmentDto(
                            inv.getId(),
                            inv.getAmount(),
                            lr.getId(),
                            lr.getStatus(),
                            lr.getAmount(),
                            funded,
                            loanId,
                            inv.getCreatedAt()
                    );
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public List<GuaranteeDto> guaranteesForLoan(Long loanId) {
        return loanGuaranteeRepository.findByLoanId(loanId).stream()
                .map(LoanQueryService::toGuaranteeDto)
                .toList();
    }

    private static LoanDto toLoanDto(LoanEntity loan) {
        return new LoanDto(
                loan.getId(),
                loan.getLoanRequest().getId(),
                loan.getPrincipal(),
                loan.getInterestRatePercent(),
                loan.getStartDate(),
                loan.getEndDate(),
                loan.getStatus()
        );
    }

    private static InstallmentDto toInstallmentDto(PaymentInstallmentEntity e) {
        return new InstallmentDto(
                e.getId(),
                e.getInstallmentNumber(),
                e.getAmountDue(),
                e.getDueDate(),
                e.getStatus(),
                e.getPaidAt()
        );
    }

    private static GuaranteeDto toGuaranteeDto(LoanGuaranteeEntity g) {
        return new GuaranteeDto(
                g.getId(),
                g.getLoan().getId(),
                g.getGuarantor() != null ? g.getGuarantor().getId() : null,
                g.getGuaranteeType(),
                g.getCoverageAmount(),
                g.getStatus()
        );
    }
}
